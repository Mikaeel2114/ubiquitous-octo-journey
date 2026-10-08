/*    */ package com.example.bedfight.util;
/*    */ 
/*    */ import java.io.IOException;
/*    */ import java.nio.file.CopyOption;
/*    */ import java.nio.file.FileVisitResult;
/*    */ import java.nio.file.Files;
/*    */ import java.nio.file.Path;
/*    */ import java.nio.file.SimpleFileVisitor;
/*    */ import java.nio.file.StandardCopyOption;
/*    */ import java.nio.file.attribute.BasicFileAttributes;
/*    */ import java.nio.file.attribute.FileAttribute;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ class null
/*    */   extends SimpleFileVisitor<Path>
/*    */ {
/*    */   public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
/* 23 */     Files.createDirectories(dst.resolve(src.relativize(dir).toString()), (FileAttribute<?>[])new FileAttribute[0]);
/* 24 */     return FileVisitResult.CONTINUE;
/*    */   }
/*    */ 
/*    */   
/*    */   public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
/* 29 */     if (!WorldFiles.SKIP.contains(file.getFileName().toString())) {
/* 30 */       Files.copy(file, dst.resolve(src.relativize(file).toString()), new CopyOption[] { StandardCopyOption.REPLACE_EXISTING });
/*    */     }
/*    */     
/* 33 */     return FileVisitResult.CONTINUE;
/*    */   }
/*    */ }


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfigh\\util\WorldFiles$1.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */