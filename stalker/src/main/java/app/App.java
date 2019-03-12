/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package app;
import java.util.*;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

public class App {

    public static void main(String[] args) {
        initStalker();
//        Tester t = new Tester();
//        t.test();
//        //System.out.println(new App().getGreeting());


//        JcpRequestHandler jcpRequestHandler = new JcpRequestHandler();
//        jcpRequestHandler.run();


        //System.out.println("Stalker Online");
    }

    public static void initStalker(){
        //clear chunk folder
        File chunk_folder = new File("temp/chunks/");
        File[] chunk_folder_contents = chunk_folder.listFiles();

        File temp_folder = new File("temp/toChunk/");
        File[] temp_folder_contents = temp_folder.listFiles();

        for(File f : chunk_folder_contents){
            if (!FilenameUtils.getExtension(f.getName()).equals("empty")){
                f.delete();
            }
        }
        for(File f : temp_folder_contents){
            if (!FilenameUtils.getExtension(f.getName()).equals("empty")){
                f.delete();
            }
        }

    }


}


// "chunks/test_files/000_mp4_test.mp4",
//                    "chunks/test_files/001_jpg_test.jpg",
//                    "chunks/test_files/002_png_test.png",
//                    "chunks/test_files/003_txt_test.txt",
//                    "chunks/test_files/004_mov_test.mov",
//                    "chunks/test_files/005_zip_test.zip",
//                    "chunks/test_files/006_rar_test.rar",
//                    "chunks/test_files/007_docx_test.docx",
//                    "chunks/test_files/008_pdf_test.pdf"));
