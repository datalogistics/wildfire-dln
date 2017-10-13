import java.io.*;

public class Extract {

    public static void main(String[] filename) {

        try {

            BufferedReader br = new BufferedReader(new FileReader(filename[0]));

            String fieldname = null;
            String fieldnameAlt = null;

            String line;
            while ((line = br.readLine()) != null) {
                if (fieldname == null && line.contains("FieldName:")) {
                    fieldname = line.substring(10);
                } else if (line.contains("FieldName:")) {
                    System.out.println(fieldname);
                    fieldname = line.substring(10);
                } else if (line.contains("FieldNameAlt:")) {
                    fieldnameAlt = line.substring(13);
                    System.out.println(fieldname + "," + fieldnameAlt);
                    fieldname = null;
                    fieldnameAlt = null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
