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
/*    */ import java.util.Set;
/*    */ 
/*    */ public final class WorldFiles {
/* 15 */   private static final Set<String> SKIP = Set.of("uid.dat", "session.lock");
/*    */ 
/*    */ 
/*    */   
/*    */   public static void copy(final Path src, final Path dst) throws IOException {
/* 20 */     Files.walkFileTree(src, new SimpleFileVisitor<Path>()
/*    */         {
/*    */           public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
/* 23 */             Files.createDirectories(dst.resolve(src.relativize(dir).toString()), (FileAttribute<?>[])new FileAttribute[0]);
/* 24 */             return FileVisitResult.CONTINUE;
/*    */           }
/*    */ 
/*    */           
/*    */           public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
/* 29 */             if (!WorldFiles.SKIP.contains(file.getFileName().toString())) {
/* 30 */               Files.copy(file, dst.resolve(src.relativize(file).toString()), new CopyOption[] { StandardCopyOption.REPLACE_EXISTING });
/*    */             }
/*    */             
/* 33 */             return FileVisitResult.CONTINUE;
/*    */           }
/*    */         });
/*    */   }
/*    */   
/*    */   public static void delete(Path dir) {
/* 39 */     if (!Files.exists(dir, new java.nio.file.LinkOption[0])) {
/*    */       return;
/*    */     }
/*    */     try {
/* 43 */       Files.walkFileTree(dir, new SimpleFileVisitor<Path>()
/*    */           {
/*    */             public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
/* 46 */               Files.deleteIfExists(file);
/* 47 */               return FileVisitResult.CONTINUE;
/*    */             }
/*    */ 
/*    */             
/*    */             public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
/* 52 */               Files.deleteIfExists(d);
/* 53 */               return FileVisitResult.CONTINUE;
/*    */             }
/*    */           });
/* 56 */     } catch (IOException iOException) {}
/*    */   }
/*    */ }


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfigh\\util\WorldFiles.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */