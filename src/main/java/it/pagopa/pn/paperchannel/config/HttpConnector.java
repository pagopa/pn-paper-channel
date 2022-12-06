package it.pagopa.pn.paperchannel.config;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;

public class HttpConnector {

    private HttpConnector(){
        throw new IllegalCallerException("the constructor must not called");
    }


    public static PDDocument downloadFile(String url) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();
        Call call = client.newCall(request);
        Response response = call.execute();
        assert response.body() != null;
        PDDocument document = PDDocument.load(response.body().byteStream());
        response.close();
        return document;
    }
}
