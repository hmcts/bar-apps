package uk.gov.hmcts.bar.api.converters;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import uk.gov.hmcts.bar.api.data.model.PaymentInstruction;
import uk.gov.hmcts.bar.api.data.model.PaymentInstructionReportLine;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PaymentInstructionsCsvConverter implements GenericHttpMessageConverter<List<PaymentInstruction>> {

    public static final String SEPARATOR = ",";
    public static final String EOL = "\n";
    public static final MediaType CSV_MEDIA_TYPE = new MediaType("text", "csv");
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat( "0.00" );

    @Override
    public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
        return false;
    }

    @Override
    public List<PaymentInstruction> read(Type type, Class<?> contextClass, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return null;
    }


    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public List<PaymentInstruction> read(Class<? extends List<PaymentInstruction>> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return null;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(CSV_MEDIA_TYPE);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return false;

    }

    @Override
    public boolean canWrite(Type type, Class<?> clazz, MediaType mediaType) {
        return Collection.class.isAssignableFrom(clazz) &&
            ((ParameterizedType) type).getActualTypeArguments()[0] == PaymentInstruction.class &&
            (mediaType == null || mediaType.isCompatibleWith(CSV_MEDIA_TYPE));
    }

    @Override
    public void write(List<PaymentInstruction> paymentInstructions, Type type, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        write(paymentInstructions, contentType, outputMessage);
    }

    @Override
    public void write(List<PaymentInstruction> paymentInstructions, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        OutputStream outputStream = outputMessage.getBody();
        outputStream.write(convertToCsv(flattenEntity(paymentInstructions)).getBytes());
        outputStream.close();
    }

    private String convertToCsv(List<String[]> data){
        StringBuilder sb = new StringBuilder();
        data.forEach(line -> sb.append(convertLine(line)).append(EOL));
        return sb.toString();
    }

    private List<String[]> flattenEntity(List<PaymentInstruction> paymentInstructions) {
        List<String[]> paymentLines = new ArrayList<>();
        paymentLines.add(PaymentInstruction.CSV_TABLE_HEADER);
        for (PaymentInstruction paymentInstruction : paymentInstructions){
            List<PaymentInstructionReportLine> flattened = paymentInstruction.flattenPaymentInstruction();
            flattened.forEach(paymentInstructionReportLine -> paymentLines.add(convertReportCellToString(paymentInstructionReportLine)));
        }
        return paymentLines;
    }

    private String[] convertReportCellToString(PaymentInstructionReportLine line){
        String[] csvRow = new String[13];
        csvRow[0] = line.getDailyId() == null ? null : line.getDailyId().toString();
        csvRow[1] = line.getDate() == null ? null : line.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        csvRow[2] = line.getName();
        csvRow[3] = formatNumber(line.getCheckAmount());
        csvRow[4] = formatNumber(line.getPostalOrderAmount());
        csvRow[5] = formatNumber(line.getCashAmount());
        csvRow[6] = formatNumber(line.getCardAmount());
        csvRow[7] = formatNumber(line.getAllPayAmount());
        csvRow[8] = line.getAction();
        csvRow[9] = line.getCaseRef();
        csvRow[10] = formatNumber(line.getFeeAmount());
        csvRow[11] = line.getFeeCode();
        csvRow[12] = line.getFeeDescription();
        return csvRow;
    }

    private String formatNumber(Integer amount){
        return amount == null ? null : DECIMAL_FORMAT.format(amount / 100d);
    }

    private String convertLine(String[] line){
        return Arrays.stream(line).reduce("", (s, s2) -> s + SEPARATOR + (s2 == null ? "\"\"" : replaceSeparator(s2))).substring(1);
    }

    /**
     * We need to use comma as separator to be able to open correctly in Excel, So we have to double quote the content
     * @param source
     * @return
     */
    private String replaceSeparator(String source){
        return "\"" + source.replaceAll("\"", "\"\"") + "\"";
    }
}