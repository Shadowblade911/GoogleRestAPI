import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;
import java.io.FileInputStream;

import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.*;
import com.google.api.services.drive.Drive;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;


public class DriveLoader {




    /** Application name. */
    private static final String APPLICATION_NAME =
        "Drive API Java Quickstart";

    private static final String FOLDER_FOR_CSV =
        "C:\\Users\\{{user}}\\Documents\\csvfiles";

    private static final String FOLDER_FOR_DOWNLOADS =
        "C:\\Users\\{{user}}\\Documents\\input";
    
    private static final String SECRET_LOCATION =
        "C:\\Users\\{{user}}\\Documents\\clProject\\src\\main\\resources";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
        System.getProperty("user.home"), ".credentials/drive-api-quickstart");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY =
        JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of the scopes required by this quickstart. */
    private static final List<String> SCOPES =
        Arrays.asList(DriveScopes.DRIVE);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }



    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in =
           new java.io.FileInputStream(SECRET_LOCATION + "/client_secret.json");
        GoogleClientSecrets clientSecrets =
            GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(DATA_STORE_FACTORY)
                .setAccessType("offline")
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(
            flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /**
     * Build and return an authorized Drive client service.
     * @return an authorized Drive client service
     * @throws IOException
     */
    public static Drive getDriveService() throws IOException {
        Credential credential = authorize();
        return new Drive.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static List<String> listFilesForFolder(final java.io.File folder) {
       List<String> ret = new ArrayList<String>();
        for (final java.io.File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                ret.addAll(listFilesForFolder(fileEntry));
            } else {
                ret.add(fileEntry.getName());
            }
        }

        return ret;
    }

     /**
       * Download a file's content.
       *
       * @param service Drive API service instance.
       * @param file Drive File instance.
       * @return InputStream containing the file's content if successful,
       *         {@code null} otherwise.
       */
      private static InputStream downloadFile(Drive service, File file) {
        //Need to use this in order to get the download link, since the file was converted into Google's native format.
        String exportUrl = file.getExportLinks().get("text/csv");
        if (exportUrl != null) {
          try {
            HttpResponse resp =
                service.getRequestFactory()
                    .buildGetRequest(new GenericUrl(exportUrl))
                        .execute();
             return resp.getContent();
          } catch (IOException e) {
            e.printStackTrace();
            return null;
          }
        } else {
          // The file doesn't have any content stored on Drive.
          return null;
        }
      }
    
    private static void writeFileFromInputStream(InputStream stream, String title) 
      throws IOException {
        java.io.File targetFile = new java.io.File(FOLDER_FOR_DOWNLOADS + "\\" + title  );
        java.io.OutputStream outStream = new java.io.FileOutputStream(targetFile);
     
        byte[] buffer = new byte[8 * 1024];
        int bytesRead;
        while ((bytesRead = stream.read(buffer)) != -1) {
            outStream.write(buffer, 0, bytesRead);
        }
        System.out.println("Downloaded file: " + title);
        stream.close();
        outStream.close();
    }

    public static void main(String[] args) throws IOException {
        // Build a new authorized API client service.
        Drive service = getDriveService();
        File gkFolder = null;
      
       
        FileList files = service.files().list().execute();

        java.util.List<File> fList = files.getItems();
       
        //Find the GK folder
        for(int i = 0; i < fList.size(); i++){
            File f= fList.get(i);
            if(f.getTitle().equals("gk")){
                gkFolder = f;
            }
        }



        //Construct folder if not found. Should not be needed. 
        if(gkFolder == null){
            File folder = new File();
            folder.setMimeType("application/vnd.google-apps.folder");
            folder.setTitle("gk");
            folder.setParents(Arrays.asList(new ParentReference().setId("root")));

            Drive.Files.Insert insert = service.files().insert(folder);
            gkFolder = insert.execute();
          }



          //Begin getting children setup
          ChildList  children = service.children().list(gkFolder.getId()).execute();
          java.util.List<ChildInfo> fNames = new ArrayList<ChildInfo>();
          //Getting children set up completed.

          //For each child found in the child list
          for(ChildReference child : children.getItems()){
            //Get Child from google
            File chil = service.files().get(child.getId()).execute();
            //Save the title and the id of the child to a list. 
            fNames.add(new ChildInfo(chil.getId(), chil.getTitle()));
          
            //At the same time download the file for storage.
            InputStream stream = downloadFile(service, chil);
            if(stream != null){
                writeFileFromInputStream(stream, chil.getTitle());
            } else {
                System.out.println("Stream was null: " + chil.getTitle());
            }
          }
 
        


        java.io.File containing  = new java.io.File(FOLDER_FOR_CSV); 
        
        List<String> FilesToLoad =  listFilesForFolder(containing);


        for(int i = 0; i < FilesToLoad.size(); i++){
            File body = new File();
            body.setTitle(FilesToLoad.get(i));
            body.setMimeType("text/csv");

           /* Sounds like we don't actually need this.
            Permission permission = new Permission();
            permission.setValue("greg.gonsior@centriam.com");
            permission.setType("user");
            permission.setRole("writer");
            */

          

            if(gkFolder != null){
                body.setParents(Arrays.asList(new ParentReference().setId(gkFolder.getId())));
            }

            java.io.File fileContent = new java.io.File(FOLDER_FOR_CSV + "\\" + FilesToLoad.get(i));

            if(fileContent.exists()){
                FileContent mediaContent = new FileContent("text/csv", fileContent);

                boolean exists = false;
                ChildInfo existing = new ChildInfo();
                for(int x = 0; x < fNames.size(); x++){
                    exists = body.getTitle().equals(fNames.get(x).getName());
                    if(exists) {
                      existing =  fNames.get(x); 
                      break;  
                    } 
                }

                if(!exists){
                    Drive.Files.Insert inserted = service.files().insert(body, mediaContent);
                    inserted.setConvert(true);
                    File f = inserted.execute();
                    System.out.println("inserted file with File ID: " + f.getId());

                    //service.permissions().insert(f.getId(), permission).execute();
                } else {
       
                 Drive.Files.Update update = service.files().update(existing.getId(), body, mediaContent);
                 update.setConvert(true);
                 File f =  update.execute();
                 System.out.println("Updated file with File ID: " + f.getId());
                
                }
                
            } else {
                System.out.println("File not found");
            }

        }

    }

}