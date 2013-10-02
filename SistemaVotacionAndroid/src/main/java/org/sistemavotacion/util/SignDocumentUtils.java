package org.sistemavotacion.util;

/*
 * Copyright 2011 - Jose. J. García Zornoza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.util.Log;

import com.itextpdf.text.pdf.PdfReader;

import org.sistemavotacion.android.AppData;
import org.sistemavotacion.android.R;
import org.sistemavotacion.android.service.ServiceListener;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.task.GetDataTask;
import org.sistemavotacion.task.SendFileTask;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import static org.sistemavotacion.android.AppData.ALIAS_CERT_USUARIO;

public class SignDocumentUtils {

    public static final String TAG = "SignDocumentUtils";

    public static void processPDFSignature (Integer requestId,String urlDocumentToSign,
                                     String urlToSendSignedDocument, byte[] keyStoreBytes,
                                     char[] password, ServiceListener serviceListener,
                                     Context context) throws Exception {
        Log.d(TAG + ".processPDFSignature(...)", " - processPDFSignature");

        GetDataTask getDataTask = new GetDataTask(AppData.PDF_CONTENT_TYPE);
        getDataTask.execute(urlDocumentToSign);
        Respuesta respuesta = getDataTask.get();
        if(Respuesta.SC_OK != respuesta.getCodigoEstado()) {
            serviceListener.proccessResponse(requestId, respuesta);
            return;
        }

		/*File root = Environment.getExternalStorageDirectory();
		File pdfFirmadoFile = new File(root,
				MainActivity.MANIFEST_FILE_NAME + "_" + evento.getEventoId() +".pdf");*/
        try {
            File pdfFirmadoFile = File.createTempFile("pdfSignedDocument", ".pdf");
            pdfFirmadoFile.deleteOnExit();
            Log.d(TAG + ".signPDF(...)", " - pdfFirmadoFile path: " + pdfFirmadoFile.getAbsolutePath());
            PdfReader pdfReader;
            pdfReader = new PdfReader(respuesta.getMessageBytes());
            KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
            PrivateKey key = (PrivateKey)keyStore.getKey(ALIAS_CERT_USUARIO, password);
            Certificate[] chain = keyStore.getCertificateChain(ALIAS_CERT_USUARIO);
            PdfUtils.firmar(pdfReader, new FileOutputStream(pdfFirmadoFile), key, chain);
            SendFileTask sendFileTask = (SendFileTask) new SendFileTask(
                    pdfFirmadoFile).execute(urlToSendSignedDocument);
            respuesta = sendFileTask.get();
            if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                serviceListener.proccessResponse(requestId, new Respuesta(
                        Respuesta.SC_OK, context.getString(R.string.operacion_ok_msg)));
            } else {
                serviceListener.proccessResponse(requestId, new Respuesta(
                        Respuesta.SC_ERROR, respuesta.getMensaje()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            serviceListener.proccessResponse(requestId, new Respuesta(
                    Respuesta.SC_ERROR, ex.getMessage()));
        }

    }
}
