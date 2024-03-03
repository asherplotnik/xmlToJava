package com.generator.xmlToJava.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ProcessXMLAndGenerateZip {

    public String processXMLAndGenerateZip(MultipartFile file) {
        try {
            InputStream inputStream = file.getInputStream();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            document.getDocumentElement().normalize();
            List<String> filesListToZip = new ArrayList<>();
            return createZipFile(document.getDocumentElement(), filesListToZip, true);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private String createZipFile(Element documentElement, List<String> filesListToZip, Boolean isRoot) throws IOException {
        List<String> childrenNames = new ArrayList<>();
        var className = upper(documentElement.getNodeName());
        NodeList children = documentElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (hasChildren(node) && !childrenNames.contains(upper(node.getNodeName()))) {
                Element childElement = (Element) node;
                childrenNames.add(upper(node.getNodeName()));
                filesListToZip.add(createZipFile(childElement, filesListToZip, false));
            } else {
                if (childrenNames.contains(upper(node.getNodeName()))) {
                    childrenNames.add(upper(node.getNodeName()));
                    continue;
                }
                if (!node.getNodeName().equals("#text")) {
                    childrenNames.add(node.getNodeName());
                }
            }
        }
        if (isRoot) {
            filesListToZip.add(createFile(className, childrenNames));
            return makeZip(filesListToZip);
        }
        return createFile(className, childrenNames);
    }

    private static boolean hasChildren(Node node) {
        if (node.getChildNodes().getLength() > 1) {
           return true;
        } else {
            if (node.getChildNodes().getLength() == 1 && node.getChildNodes().item(0).getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }
        return false;
    }

    private String createFile(String className, List<String> childrenNames) {
        Map<String, Long> mapByName = childrenNames.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        String filePath = "C:/Users/ASHER/git/xmlToJava/xmlToJava/src/main/resources/static/temp-files/" + className + ".java";
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("@Data");
            writer.newLine();
            writer.write("@Builder");
            writer.newLine();
            writer.write("@NoArgsConstructor");
            writer.newLine();
            writer.write("public class " + className + " {");
            writer.newLine();
            mapByName.forEach((key, value) -> {
                try {
                    String listDec1 = "";
                    String listDec2 = "";
                    String field = isFirstLetterUpperCase(key) ? key : "String";
                    if (value > 1) {
                        listDec1 = "List<";
                        listDec2 = ">";
                    }
                    writer.write("    private " + listDec1 + field + listDec2 + " " + key + ";");
                    writer.newLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            writer.write("}");
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return filePath;
    }

    public static boolean isFirstLetterUpperCase(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return Character.isUpperCase(str.charAt(0));
    }

    private String upper(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String lower(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    private String makeZip(List<String> filesListToZip) throws IOException {
        byte[] buffer = new byte[1024];
        String outputZipPath = "C:/Users/ASHER/git/xmlToJava/xmlToJava/src/main/resources/static/temp-files/outputZipFile.zip";
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputZipPath))) {
            for (String filePath : filesListToZip) {
                try (FileInputStream fis = new FileInputStream(filePath)) {
                    ZipEntry zipEntry = new ZipEntry(filePath.substring(filePath.lastIndexOf('/') + 1));
                    zos.putNextEntry(zipEntry);
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                } catch (IOException e) {
                    System.err.println("Error processing file: " + filePath);
                    e.printStackTrace();
                }
            }
            return outputZipPath;
        } catch (IOException e) {
            System.err.println("Error creating ZIP file: " + outputZipPath);
            e.printStackTrace();
            throw e; // Re-throw the exception to indicate failure
        }
    }

}
