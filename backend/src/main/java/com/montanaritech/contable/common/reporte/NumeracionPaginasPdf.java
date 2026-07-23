package com.montanaritech.contable.common.reporte;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import java.io.IOException;

/**
 * "Página X de Y" en el pie de cada página (F7.1). El total de páginas no se
 * conoce hasta cerrar el documento, así que se reserva un template en blanco
 * en cada página ({@link #onEndPage}) y se completa recién al final
 * ({@link #onCloseDocument}) — técnica estándar de OpenPDF/iText para este
 * caso.
 */
class NumeracionPaginasPdf extends PdfPageEventHelper {

    private PdfTemplate totalPaginasTemplate;
    private BaseFont fuente;

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        try {
            fuente = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("No se pudo crear la fuente para la numeración de páginas", e);
        }
        totalPaginasTemplate = writer.getDirectContent().createTemplate(30, 16);
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte contenido = writer.getDirectContent();
        String texto = "Página " + writer.getPageNumber() + " de ";
        float ancho = fuente.getWidthPoint(texto, 8);
        float x = document.right() - ancho - 30;
        float y = document.bottom() - 20;

        contenido.beginText();
        contenido.setFontAndSize(fuente, 8);
        contenido.showTextAligned(Element.ALIGN_LEFT, texto, x, y, 0);
        contenido.endText();
        contenido.addTemplate(totalPaginasTemplate, x + ancho, y);
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        totalPaginasTemplate.beginText();
        totalPaginasTemplate.setFontAndSize(fuente, 8);
        totalPaginasTemplate.setTextMatrix(0, 0);
        totalPaginasTemplate.showText(String.valueOf(writer.getPageNumber() - 1));
        totalPaginasTemplate.endText();
    }
}
