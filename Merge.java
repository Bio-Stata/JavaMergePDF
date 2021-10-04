package MergePDFs;
//
// Java Program for combining PDF files using iText7
// Version : 04OCT21
// Author  : Peter Lipczak
//

import com.itextpdf.forms.PdfPageFormCopier;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfOutline;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfViewerPreferences;
import com.itextpdf.kernel.pdf.ReaderProperties;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.canvas.draw.DottedLine;
import com.itextpdf.kernel.pdf.navigation.PdfDestination;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Tab;
import com.itextpdf.layout.element.TabStop;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.property.TabAlignment;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.VerticalAlignment;



import java.io.BufferedReader;
import java.io.FileReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.lang.Math;


public class Merge {
     // Document orientation specified in argument
     public static String Orientation;
     // Path of input folder
     public static String TocFile;
     // name and path of output file
     public static String OutFile;
     // string holding filenames  
     public static ArrayList<String> InFiles = new ArrayList<>();
     // string holding titles
     public static ArrayList<String> InTitles = new ArrayList<>();
          //public static String[] InFiles = new String[50];
     
      public static void main(String[] args) throws IOException {
      
      // args[0] : Input folder
      //InFolder  = "C:\\Dev\\JavaPDFMerge\\InputPDFs";
      // read files and titles from # sperated file
      BufferedReader reader;
      try {
          reader = new BufferedReader(new FileReader(args[1]));
          String line = reader.readLine();
          while (line != null) {
              String[] lineParts = line.split("#");
              InFiles.add(lineParts[0]);
              InTitles.add(lineParts[1]); 
              line = reader.readLine();
          }
          reader.close();
      } catch (IOException e) {
          e.printStackTrace();
      }
      
      // string holding name and path of TOC file
      TocFile = args[0];
      // String holding output filename 
      //OutFile = "C:\\Dev\\JavaPDFMerge\\Output\\MergedFiles.pdf";
       OutFile = args[2];
      // Document orientation
      Orientation = args[3]; 

       File file = new File(Merge.OutFile);
       file.getParentFile().mkdirs();

        new Merge().mergePDFFiles(OutFile);
       

    }


    protected void mergePDFFiles(String dest) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
        // setting landscape if specified
        if(Orientation=="Landscape"){
            pdfDoc.setDefaultPageSize(PageSize.A4.rotate());
        }
        else {
            pdfDoc.setDefaultPageSize(PageSize.A4);
        }
        
        Document doc = new Document(pdfDoc);
        // Copier contains the additional logic to copy acroform fields to a new page.
        // PdfPageFormCopier uses some caching logic which can potentially improve performance
        // in case of the reusing of the same instance.
        PdfPageFormCopier formCopier = new PdfPageFormCopier();

        // Copy all merging file's pages to the temporary pdf file
        Map<String, PdfDocument> filesToMerge = initializeFilesToMerge();
        Map<Integer, String> toc = new TreeMap<Integer, String>();
        int page = 1;
        for (Map.Entry<String, PdfDocument> entry : filesToMerge.entrySet()) {
            PdfDocument srcDoc = entry.getValue();
            int numberOfPages = srcDoc.getNumberOfPages();

            toc.put(page, entry.getKey());

            for (int i = 1; i <= numberOfPages; i++, page++) {
                Text text = new Text(String.format("Page %d", page));
                
                srcDoc.copyPagesTo(i, i, pdfDoc, formCopier);
               
                // Put the destination at the very first page of each merged document
                if (i == 1) {
                    text.setDestination("p" + page);

                    PdfOutline rootOutLine = pdfDoc.getOutlines(false);
                    PdfOutline outline = rootOutLine.addOutline("p" + page);
                    outline.addDestination(PdfDestination.makeDestination(new PdfString("p" + page)));
                }

                doc.add(new Paragraph(text)
                        .setFixedPosition(page, 549, 810, 40)
                        .setMargin(0)
                        .setMultipliedLeading(1));
            }
        }
       // use a number of pages in toc document as front pages
       // The number of pages are based on number of toc lines.
       // 22 lines per page is assumed.

        PdfDocument tocDoc = new PdfDocument(new PdfReader(TocFile));
        // derive no of pages needed. First page is the front page
        int noTocPages = (int) Math.ceil(filesToMerge.size()/22.0)+1;
        tocDoc.copyPagesTo(1, noTocPages, pdfDoc, formCopier);
        tocDoc.close();

        int tocStartPage = pdfDoc.getNumberOfPages() - noTocPages + 1;
        int tocEndPage = pdfDoc.getNumberOfPages();
        
        float tocXCoordinate = doc.getLeftMargin()+30;

        float tocWidth = pdfDoc.getDefaultPageSize().getWidth() - doc.getLeftMargin() - doc.getRightMargin();
        
        // Adjust if landscape
        if(Orientation.equals("Landscape")){
            tocWidth = pdfDoc.getDefaultPageSize().getHeight() - doc.getLeftMargin() - doc.getRightMargin()-75;
        }
        
        int tocPage = tocStartPage+1;
        int lineno = 0 ;
        int pageno = 1;
         // Create a table of contents
         
         float tocYCoordinate = 420;
         for (Map.Entry<Integer, String> entry : toc.entrySet()) {
            lineno++; 
            Paragraph p = new Paragraph();
            p.setFontSize(9);
            p.addTabStops(new TabStop(tocWidth , TabAlignment.RIGHT, new DottedLine()));
            // remove prefix numbering
            p.add(entry.getValue());
            p.add(new Tab());
            // we need to adjust displayed toc page number
            p.add(String.valueOf(entry.getKey()+noTocPages));           
            p.setAction(PdfAction.createGoTo("p" + entry.getKey()));
                        
            doc.add(p
                    .setFixedPosition(tocPage, tocXCoordinate, tocYCoordinate, tocWidth)
                    .setMargin(0)
                    .setMultipliedLeading(1));
            tocYCoordinate -= 14;
            // goto new page when needed
            if(lineno>pageno*22){
                tocPage++;
                pageno++;
                tocYCoordinate = 420;
            }
            
         }
           
          
       // closing input documents
        for (PdfDocument srcDoc : filesToMerge.values()) {
            srcDoc.close();
        }

        doc.close();
       // create final document
        PdfDocument resultDoc = new PdfDocument(new PdfWriter(dest));
        PdfDocument srcDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(baos.toByteArray()),
                new ReaderProperties()));
        srcDoc.initializeOutlines();

        // Create a copy order list and set the page with a table of contents as the first page(s)
        // as toc pages are added to the end of the document we need to move them to the beginning 
        // of the document
        //int tocPageNumber = srcDoc.getNumberOfPages();
        List<Integer> copyPagesOrderList = new ArrayList<>();
        // 'move' toc pages to front
        for(int i= tocStartPage; i<=tocEndPage;i++){
           copyPagesOrderList.add(i);
        }
        
        
        for(int i = 1; i < tocStartPage; i++) {
            copyPagesOrderList.add(i);
        }

        srcDoc.copyPagesTo(copyPagesOrderList, resultDoc, formCopier);

        srcDoc.close();
        //
        // Creating page x of y in final document
        //
        Document resDoc = new Document(resultDoc);
        int numberOfPages = resultDoc.getNumberOfPages();
      
                        
                        
        PdfDocumentInfo info = resultDoc.getDocumentInfo();
        info.setTitle("Merged PDF");
        

        for (int i = 1; i <= numberOfPages; i++) {
           // Write aligned text to the specified by parameters point
            resDoc.showTextAligned(new Paragraph(String.format("Page %s of %s", i, numberOfPages)).setFontSize(8),
                                775, 75, i, TextAlignment.RIGHT, VerticalAlignment.TOP, 0);
        }
        resDoc.close();
        resultDoc.close();
    }

    private static Map<String, PdfDocument> initializeFilesToMerge() throws IOException {
        Map<String, PdfDocument> filesToMerge = new TreeMap<String, PdfDocument>();
        for(int i = 0; i < InFiles.size(); i++)
        {
           filesToMerge.put(String.format("%03d", i)+InTitles.get(i), new PdfDocument(new PdfReader(InFiles.get(i))));
        }
        return filesToMerge;
    }

}
