package org.example;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private volatile LocalDateTime start;
    private AtomicInteger count = new AtomicInteger(0);

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
    }

    public void process(Document document, String crypt) throws RuntimeException {
        if (count.get() == 0)
            start = LocalDateTime.now();
        synchronized (this) {
            if (expired()) {
                count = new AtomicInteger(0);
                start = LocalDateTime.now();
            }
        }
        int current = count.getAndIncrement();
        if (current >= requestLimit) {
            count.decrementAndGet();
            System.out.println("Blocked");
            return;
        }
        request(document, crypt);
    }

    public void request(Document document, String crypt) {
        Request request = new Request(document, crypt);
        new Thread(request).start();
    }

    private boolean expired() {
        return LocalDateTime.now().isAfter(start.plusSeconds(timeUnit.toSeconds(1)));
    }


    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5);
        Production production = new Production("test", LocalDate.now(), "test", "test", LocalDate.now(), "test", "test", "test");
        Document document = new Document(new Description("test"), "test", "test", "test", true, "test", "test", "test", LocalDate.now(), "test", new HashSet<>(List.of(new Production[]{production})), LocalDate.now(), "test");
        try {
            crptApi.process(document, "test");
            crptApi.process(document, "test");
            crptApi.process(document, "test");
            crptApi.process(document, "test");
            crptApi.process(document, "test");
            crptApi.process(document, "test");
            Thread.sleep(1000);
            crptApi.process(document, "test");
            crptApi.process(document, "test");
            crptApi.process(document, "test");
            crptApi.process(document, "test");
            crptApi.process(document, "test");
            crptApi.process(document, "test");
            crptApi.process(document, "test");
            crptApi.process(document, "test");
            crptApi.process(document, "test");
            crptApi.process(document, "test");
        } catch (RuntimeException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    class Request implements Runnable {
        private final Document document;
        private final String crypt;

        Request(Document document, String crypt) {
            this.document = document;
            this.crypt = crypt;
        }

        private JSONObject parseProduct(Production production) {
            JSONObject jsonProduct = new JSONObject();
            jsonProduct.put("production_date", production.getProductionDate() != null ? production.getProductionDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
            jsonProduct.put("certificate_document", production.getCertificateDocument());
            jsonProduct.put("certificate_document_date", production.getCertificateDocumentDate() != null ? production.getCertificateDocumentDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
            jsonProduct.put("tnved_code", production.getTnvedCode());
            jsonProduct.put("uitu_code", production.getUituCode());
            jsonProduct.put("uit_code", production.getUitCode());
            jsonProduct.put("owner_inn", production.getOwnerInn());
            jsonProduct.put("certificate_document_number", production.getCertificateDocumentNumber());
            return jsonProduct;
        }

        private JSONObject parseDocument(Document document) {
            JSONObject jsonDocument = new JSONObject();
            JSONObject jsonDescription = new JSONObject();
            jsonDescription.put("participant_inn", document.getDescription().getParticipantInn());
            jsonDocument.put("description", jsonDescription);
            jsonDocument.put("doc_id", document.getDocId());
            jsonDocument.put("doc_status", document.getDocStatus());
            jsonDocument.put("doc_type", document.getDocType());
            jsonDocument.put("import_request", document.isImportRequest());
            jsonDocument.put("owner_inn", document.getOwnerInn());
            jsonDocument.put("participant_inn", document.getParticipantInn());
            jsonDocument.put("producer_inn", document.getProducerInn());
            jsonDocument.put("production_date", document.getProductionDate() != null ? document.getProductionDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
            jsonDocument.put("production_type", document.getProductionType());
            JSONArray productsArray = new JSONArray();
            if (document.getProducts() != null) {
                for (Production product : document.getProducts()) {
                    productsArray.put(parseProduct(product));
                }
            }
            jsonDocument.put("products", productsArray);
            jsonDocument.put("reg_date", document.getRegDate() != null
                    ? document.getRegDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    : null);
            jsonDocument.put("reg_number", document.getRegNumber());

            return jsonDocument;
        }

        @Override
        public void run() {
            System.out.println("Sending");
            JSONObject body = new JSONObject();
            body.put("document", parseDocument(document));
            body.put("crypt", crypt);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://test.com"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println(response.body());
            } catch (IOException | InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    static class Document {
        private Description description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private LocalDate productionDate;
        private String productionType;
        private Set<Production> products;
        private LocalDate regDate;
        private String regNumber;

        @Override
        public String toString() {
            return "Document{" +
                    "description=" + description +
                    ", docId='" + docId + '\'' +
                    ", docStatus='" + docStatus + '\'' +
                    ", docType='" + docType + '\'' +
                    ", importRequest=" + importRequest +
                    ", ownerInn='" + ownerInn + '\'' +
                    ", participantInn='" + participantInn + '\'' +
                    ", producerInn='" + producerInn + '\'' +
                    ", productionDate=" + productionDate +
                    ", productionType='" + productionType + '\'' +
                    ", products=" + products +
                    ", regDate=" + regDate +
                    ", regNumber='" + regNumber + '\'' +
                    '}';
        }

        public Document() {
        }

        public Document(Description description, String docId, String docStatus, String docType, boolean importRequest, String ownerInn, String participantInn, String producerInn, LocalDate productionDate, String productionType, Set<Production> products, LocalDate regDate, String regNumber) {
            this.description = description;
            this.docId = docId;
            this.docStatus = docStatus;
            this.docType = docType;
            this.importRequest = importRequest;
            this.ownerInn = ownerInn;
            this.participantInn = participantInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.productionType = productionType;
            this.products = products;
            this.regDate = regDate;
            this.regNumber = regNumber;
        }

        public Description getDescription() {
            return description;
        }

        public void setDescription(Description description) {
            this.description = description;
        }

        public String getDocId() {
            return docId;
        }

        public void setDocId(String docId) {
            this.docId = docId;
        }

        public String getDocStatus() {
            return docStatus;
        }

        public void setDocStatus(String docStatus) {
            this.docStatus = docStatus;
        }

        public String getDocType() {
            return docType;
        }

        public void setDocType(String docType) {
            this.docType = docType;
        }

        public boolean isImportRequest() {
            return importRequest;
        }

        public void setImportRequest(boolean importRequest) {
            this.importRequest = importRequest;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public void setOwnerInn(String ownerInn) {
            this.ownerInn = ownerInn;
        }

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }

        public String getProducerInn() {
            return producerInn;
        }

        public void setProducerInn(String producerInn) {
            this.producerInn = producerInn;
        }

        public LocalDate getProductionDate() {
            return productionDate;
        }

        public void setProductionDate(LocalDate productionDate) {
            this.productionDate = productionDate;
        }

        public String getProductionType() {
            return productionType;
        }

        public void setProductionType(String productionType) {
            this.productionType = productionType;
        }

        public Set<Production> getProducts() {
            return products;
        }

        public void setProducts(Set<Production> products) {
            this.products = products;
        }

        public LocalDate getRegDate() {
            return regDate;
        }

        public void setRegDate(LocalDate regDate) {
            this.regDate = regDate;
        }

        public String getRegNumber() {
            return regNumber;
        }

        public void setRegNumber(String regNumber) {
            this.regNumber = regNumber;
        }
    }

    static class Description {

        public Description(String participantInn) {
            this.participantInn = participantInn;
        }

        public Description() {
        }

        private String participantInn;

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }
    }

    static class Production {
        private String certificateDocument;
        private LocalDate certificateDocumentDate;
        private String certificateDocumentNumber;
        private String ownerInn;
        private LocalDate productionDate;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;

        public Production(String certificateDocument, LocalDate certificateDocumentDate, String certificateDocumentNumber, String ownerInn, LocalDate productionDate, String tnvedCode, String uitCode, String uituCode) {
            this.certificateDocument = certificateDocument;
            this.certificateDocumentDate = certificateDocumentDate;
            this.certificateDocumentNumber = certificateDocumentNumber;
            this.ownerInn = ownerInn;
            this.productionDate = productionDate;
            this.tnvedCode = tnvedCode;
            this.uitCode = uitCode;
            this.uituCode = uituCode;
        }

        public String getCertificateDocument() {
            return certificateDocument;
        }

        public void setCertificateDocument(String certificateDocument) {
            this.certificateDocument = certificateDocument;
        }

        public LocalDate getCertificateDocumentDate() {
            return certificateDocumentDate;
        }

        public void setCertificateDocumentDate(LocalDate certificateDocumentDate) {
            this.certificateDocumentDate = certificateDocumentDate;
        }

        public String getCertificateDocumentNumber() {
            return certificateDocumentNumber;
        }

        public void setCertificateDocumentNumber(String certificateDocumentNumber) {
            this.certificateDocumentNumber = certificateDocumentNumber;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public void setOwnerInn(String ownerInn) {
            this.ownerInn = ownerInn;
        }

        public LocalDate getProductionDate() {
            return productionDate;
        }

        public void setProductionDate(LocalDate productionDate) {
            this.productionDate = productionDate;
        }

        public String getTnvedCode() {
            return tnvedCode;
        }

        public void setTnvedCode(String tnvedCode) {
            this.tnvedCode = tnvedCode;
        }

        public String getUitCode() {
            return uitCode;
        }

        public void setUitCode(String uitCode) {
            this.uitCode = uitCode;
        }

        public String getUituCode() {
            return uituCode;
        }

        public void setUituCode(String uituCode) {
            this.uituCode = uituCode;
        }

        @Override
        public String toString() {
            return "Production{" +
                    "certificateDocument='" + certificateDocument + '\'' +
                    ", certificateDocumentDate=" + certificateDocumentDate +
                    ", certificateDocumentNumber='" + certificateDocumentNumber + '\'' +
                    ", ownerInn='" + ownerInn + '\'' +
                    ", productionDate=" + productionDate +
                    ", tnvedCode='" + tnvedCode + '\'' +
                    ", uitCode='" + uitCode + '\'' +
                    ", uituCode='" + uituCode + '\'' +
                    '}';
        }
    }

}

