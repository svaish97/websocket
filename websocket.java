public class SocketTextHandler extends TextWebSocketHandler {
   private static int linesToRead = 10;

   static {
   }

   @Override
   public void handleTextMessage(WebSocketSession session, TextMessage message)
         throws InterruptedException, IOException {
      System.out.println("init");
      Path logFilePath = Paths.get("/home/saurabh/Downloads/CancelFromBillSystem_Q161.log");
      try {
         watchLogFile(session,logFilePath);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   @Async
   private static void watchLogFile(WebSocketSession session,Path logFilePath) throws IOException {
      try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
         logFilePath.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

         System.out.println("Watching for changes in the log file...");
         List<String> lines = readLastLines(logFilePath, linesToRead);
         for (String line : lines) {
            session.sendMessage(new TextMessage(line));
            System.out.println(line);
         }

         while (true) {
            WatchKey key;
            try {
               key = watchService.take();
            } catch (InterruptedException e) {
               System.err.println("Error while waiting for file changes: " + e.getMessage());
               return;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
               if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                  Path modifiedFile = (Path) event.context();
                  if (modifiedFile.equals(logFilePath.getFileName())) {
                     System.out.println("Log file modified. Reading lines...");

                     lines = readLastLines(logFilePath, linesToRead);
                     for (String line : lines) {
                        session.sendMessage(new TextMessage(line));
                        System.out.println(line);
                     }
                     if (linesToRead > 1) {
                        linesToRead = 1;
                     }
                  }
               }
            }
            boolean valid = key.reset();
            if (!valid) {
               break;
            }
         }
      }
   }

   private static List<String> readLastLines(Path filePath, int numLines) throws IOException {
      BufferedReader reader = null;
      try {
         reader = new BufferedReader(new FileReader(filePath.toFile()));
         LinkedList<String> lastLines = new LinkedList<>();

         String line;
         while ((line = reader.readLine()) != null) {
            lastLines.add(line);
            if (lastLines.size() > numLines) {
               lastLines.removeFirst();
            }
         }

         return new ArrayList<>(lastLines);
      } finally {
         if (reader != null) {
            try {
               reader.close();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }
   }
}
