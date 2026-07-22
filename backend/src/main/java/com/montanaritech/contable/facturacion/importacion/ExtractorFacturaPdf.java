package com.montanaritech.contable.facturacion.importacion;

import com.montanaritech.contable.facturacion.TipoComprobante;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Extracción básica de texto de facturas históricas en PDF (F4.6): el paso
 * pide explícitamente "solo extracción básica de texto con PDFBox... el
 * parsing avanzado de PDF queda para la etapa 2". Esto NO es un parser
 * estructural de layouts — es un conjunto de patrones regex sobre el texto
 * lineal que devuelve PDFBox, cuyo orden de por sí NO respeta la
 * disposición visual del comprobante (verificado contra comprobantes reales:
 * en el layout estándar ARCA/AFIP para Factura B/C sin IVA discriminado, las
 * etiquetas "Subtotal:"/"Importe Total:" quedan sueltas y sus valores
 * aparecen varias líneas antes, pegados al detalle de renglones — por eso
 * {@link #extraerTotal} tiene un segundo camino además del match directo
 * "Importe Total: NNN"). Cualquier campo no detectado queda {@code null} —
 * el formulario de carga asistida lo completa a mano; nunca se confirma
 * nada automáticamente a partir de esta extracción.
 */
@Component
public class ExtractorFacturaPdf {

    private static final Set<String> PREFIJOS_CUIT_VALIDOS = Set.of("20", "23", "24", "25", "26", "27", "30", "33", "34");

    private static final Pattern PUNTO_VENTA_COMP_NRO = Pattern.compile("Punto de Venta:\\s*(\\d+)\\s*Comp\\.\\s*Nro:\\s*(\\d+)");
    /**
     * Variante real observada en el layout estándar ARCA/AFIP: PDFBox extrae
     * las DOS etiquetas juntas ("Punto de Venta: Comp. Nro:") y recién
     * después los DOS valores juntos ("00003 00000105") — el orden visual
     * (etiqueta1 valor1 etiqueta2 valor2) no sobrevive a la extracción lineal.
     */
    private static final Pattern PUNTO_VENTA_COMP_NRO_ETIQUETAS_JUNTAS = Pattern.compile(
            "Punto de Venta:\\s*Comp\\.\\s*Nro:\\s*(\\d{3,5})\\s+(\\d{4,8})");
    private static final Pattern COMPR_NRO_GUION = Pattern.compile("Compr\\.\\s*Nro:\\s*(\\d+)-(\\d+)");
    private static final Pattern NUMERO_GUION_ETIQUETADO = Pattern.compile("N[ºo°]?\\s*[:.]?\\s*(\\d{4})\\s*-\\s*(\\d{5,8})");
    private static final Pattern NUMERO_GUION_SUELTO = Pattern.compile("\\b(\\d{4})\\s*-\\s*(\\d{8})\\b");
    private static final Pattern CODIGO_AFIP = Pattern.compile("(?:COD\\.|C[oó]digo)\\s*0*(\\d{1,3})\\b");
    private static final Pattern CUIT_ETIQUETADO = Pattern.compile("CUIT:\\s*(\\d{2}-?\\d{8}-?\\d{1})\\b");
    private static final Pattern CUIT_SUELTO = Pattern.compile("\\b(\\d{2}-\\d{8}-\\d{1}|\\d{11})\\b");
    private static final Pattern CAE = Pattern.compile("CAE\\s*N?[°º]?\\.?:?\\s*(\\d{10,14})");
    /**
     * {@code \b} no alcanza acá: PDFBox a veces pega el CAE directamente a la
     * palabra siguiente sin espacio (ej. "86262004442157Pág.") — dígito+letra
     * no es un límite de palabra en regex Java (ambos son {@code \w}), así
     * que {@code \b(\d{14})\b} no matchea. Se exige en cambio que no haya
     * OTRO dígito pegado antes/después (evita matchear un sub-run dentro de
     * un número más largo).
     */
    private static final Pattern CAE_SUELTO = Pattern.compile("(?<!\\d)(\\d{14})(?!\\d)");
    private static final Pattern TIPO_CAMBIO = Pattern.compile("Tipo de Cambio:\\s*([\\d.]+)");
    private static final Pattern FECHA_DDMMYYYY = Pattern.compile("\\b(\\d{2})[/.](\\d{2})[/.](\\d{4})\\b");
    private static final Pattern FECHA_INGLES = Pattern.compile(
            "\\b(January|February|March|April|May|June|July|August|September|October|November|December)\\s+(\\d{1,2}),\\s+(\\d{4})\\b");
    private static final Pattern IVA_CONTENIDO = Pattern.compile("IVA Contenido:\\s*\\$?\\s*([\\d.,]+)");
    private static final Pattern NETO_GRAVADO = Pattern.compile("Importe Neto Gravado:\\s*\\$?\\s*([\\d.,]+)");
    private static final Pattern IVA_ALICUOTA = Pattern.compile("IVA\\s*(\\d+(?:[.,]\\d+)?)\\s*%:?\\s*\\$?\\s*([\\d.,]+)");
    private static final Pattern SUBTOTAL = Pattern.compile("(?i)SUB-?TOTAL:?\\s*\\$?\\s*([\\d.,]+)");
    /** A diferencia de "[\d.,]+" suelto, exige que el valor termine en decimal (",NN"/".NN") — evita matchear una cantidad de línea suelta como "1". */
    private static final String MONTO_DECIMAL = "(\\d+(?:[.,]\\d{3})*[.,]\\d{2})";
    private static final Pattern MONTO_TOKEN = Pattern.compile("\\b\\d+(?:\\.\\d{3})*,\\d{2}\\b");

    /** Firma de la variante ARCA "Factura B/C sin IVA discriminado": etiquetas de totales sueltas, sin valor pegado. */
    private static final Pattern ETIQUETAS_TOTAL_SUELTAS = Pattern.compile(
            "Subtotal:\\s*\\$\\s*\\R+Importe Otros Tributos:\\s*\\$\\s*\\R+Importe Total:\\s*\\$");

    private static final List<Pattern> PATRONES_TOTAL_DIRECTO = List.of(
            Pattern.compile("Importe Total:\\s*\\$?\\s*(?:USD)?\\s*" + MONTO_DECIMAL),
            // (?<!SUB)/(?<!SUB-) para no matchear "SUBTOTAL"/"SUB-TOTAL"; MONTO_DECIMAL para no matchear
            // una cantidad de línea suelta (ej. "1") cuando "TOTAL" aparece como encabezado de columna.
            Pattern.compile("(?i)(?<!SUB)(?<!SUB-)\\bTOTAL\\s*:?\\s*\\$?\\s*" + MONTO_DECIMAL));

    private static final Set<BigDecimal> ALICUOTAS_CONOCIDAS = Set.of(
            new BigDecimal("0"), new BigDecimal("2.5"), new BigDecimal("5"),
            new BigDecimal("10.5"), new BigDecimal("21"), new BigDecimal("27"));

    private final String empresaCuit;
    private final String empresaNombreFantasia;

    public ExtractorFacturaPdf(
            @Value("${app.empresa.cuit}") String empresaCuit,
            @Value("${app.empresa.nombre-fantasia}") String empresaNombreFantasia) {
        this.empresaCuit = normalizarCuit(empresaCuit);
        this.empresaNombreFantasia = empresaNombreFantasia;
    }

    public CamposExtraidosPdf extraer(String textoOriginal) {
        // PDFBox devuelve espacios de no separación (U+00A0) en varios layouts (ej. alrededor de "$"),
        // que \s de Java regex NO matchea — normalizar una sola vez acá evita parchear cada patrón.
        String texto = textoOriginal.replace(' ', ' ');
        List<String> advertencias = new ArrayList<>();

        String tipoSugerido = texto.toUpperCase(Locale.ROOT).contains(empresaNombreFantasia.toUpperCase(Locale.ROOT))
                ? "VENTA" : "COMPRA";

        TipoComprobante tipoComprobante = extraerTipoComprobante(texto, advertencias);
        String[] puntoVentaYNumero = extraerPuntoVentaYNumero(texto, advertencias);
        LocalDate fecha = extraerFecha(texto, advertencias);
        String cuitContraparte = extraerCuitContraparte(texto, advertencias);
        String monedaCodigo = texto.toUpperCase(Locale.ROOT).contains("USD") ? "USD" : "ARS";
        BigDecimal tipoCambio = extraerDecimalPunto(TIPO_CAMBIO, texto);
        BigDecimal total = extraerTotal(texto, advertencias);
        BigDecimal[] netoYAlicuota = extraerNetoYAlicuota(texto, total, advertencias);
        String cae = extraerPrimero(CAE, texto);
        if (cae == null) {
            cae = extraerPrimero(CAE_SUELTO, texto);
        }
        if (cae == null) {
            advertencias.add("No se detectó CAE — verificar si corresponde completarlo manualmente.");
        }

        return new CamposExtraidosPdf(tipoSugerido, tipoComprobante, puntoVentaYNumero[0], puntoVentaYNumero[1],
                fecha, cuitContraparte, monedaCodigo, tipoCambio, netoYAlicuota[0], netoYAlicuota[1], total, cae,
                advertencias);
    }

    private TipoComprobante extraerTipoComprobante(String texto, List<String> advertencias) {
        Matcher m = CODIGO_AFIP.matcher(texto);
        if (m.find()) {
            TipoComprobante tipo = CodigoComprobanteAfip.resolver(Integer.parseInt(m.group(1)));
            if (tipo != null) {
                return tipo;
            }
        }
        advertencias.add("No se detectó el tipo de comprobante (código ARCA/AFIP) — seleccionar manualmente.");
        return null;
    }

    private String[] extraerPuntoVentaYNumero(String texto, List<String> advertencias) {
        Matcher m = PUNTO_VENTA_COMP_NRO.matcher(texto);
        if (m.find()) {
            return new String[]{m.group(1), m.group(2)};
        }
        m = PUNTO_VENTA_COMP_NRO_ETIQUETAS_JUNTAS.matcher(texto);
        if (m.find()) {
            return new String[]{m.group(1), m.group(2)};
        }
        m = COMPR_NRO_GUION.matcher(texto);
        if (m.find()) {
            return new String[]{m.group(1), m.group(2)};
        }
        m = NUMERO_GUION_ETIQUETADO.matcher(texto);
        if (m.find()) {
            return new String[]{m.group(1), m.group(2)};
        }
        m = NUMERO_GUION_SUELTO.matcher(texto);
        if (m.find()) {
            return new String[]{m.group(1), m.group(2)};
        }
        advertencias.add("No se detectó punto de venta / número de comprobante — completar manualmente.");
        return new String[]{null, null};
    }

    private LocalDate extraerFecha(String texto, List<String> advertencias) {
        Matcher m = FECHA_DDMMYYYY.matcher(texto);
        if (m.find()) {
            int dia = Integer.parseInt(m.group(1));
            int mes = Integer.parseInt(m.group(2));
            int anio = Integer.parseInt(m.group(3));
            return LocalDate.of(anio, mes, dia);
        }
        m = FECHA_INGLES.matcher(texto);
        if (m.find()) {
            String fechaTexto = m.group(1) + " " + m.group(2) + ", " + m.group(3);
            return LocalDate.parse(fechaTexto, DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH));
        }
        advertencias.add("No se detectó la fecha de emisión — completar manualmente.");
        return null;
    }

    /**
     * Busca primero CUIT pegado a la etiqueta "CUIT:" (más confiable). Si
     * ninguno distinto de Montanari aparece así (pasa en algunos layouts
     * donde el CUIT de la contraparte queda suelto en el texto), cae a
     * buscar cualquier secuencia de 11 dígitos con prefijo de CUIT argentino
     * válido (20/23/24/25/26/27/30/33/34) — descarta así identificadores
     * fiscales extranjeros de 11 dígitos (ej. "CUIT País" de exportación)
     * que no siguen ese prefijo.
     */
    private String extraerCuitContraparte(String texto, List<String> advertencias) {
        Set<String> candidatos = candidatosCuit(CUIT_ETIQUETADO, texto);
        candidatos.remove(empresaCuit);
        if (candidatos.isEmpty()) {
            candidatos = candidatosCuit(CUIT_SUELTO, texto);
            candidatos.remove(empresaCuit);
        }
        if (candidatos.isEmpty()) {
            advertencias.add("No se detectó el CUIT de la contraparte (puede ser un cliente del exterior sin CUIT AR) — completar manualmente.");
            return null;
        }
        if (candidatos.size() > 1) {
            advertencias.add("Se detectó más de un CUIT distinto al de Montanari — verificar cuál corresponde a la contraparte.");
        }
        return candidatos.iterator().next();
    }

    private Set<String> candidatosCuit(Pattern patron, String texto) {
        Matcher m = patron.matcher(texto);
        Set<String> cuits = new LinkedHashSet<>();
        while (m.find()) {
            String normalizado = normalizarCuit(m.group(1));
            if (PREFIJOS_CUIT_VALIDOS.contains(normalizado.substring(0, 2))) {
                cuits.add(normalizado);
            }
        }
        return cuits;
    }

    private BigDecimal extraerTotal(String texto, List<String> advertencias) {
        for (Pattern patron : PATRONES_TOTAL_DIRECTO) {
            Matcher m = patron.matcher(texto);
            if (m.find()) {
                BigDecimal valor = parseMonto(m.group(1));
                if (valor != null) {
                    return valor;
                }
            }
        }
        Matcher etiquetasSueltas = ETIQUETAS_TOTAL_SUELTAS.matcher(texto);
        if (etiquetasSueltas.find()) {
            String antes = texto.substring(0, etiquetasSueltas.start());
            Matcher montos = MONTO_TOKEN.matcher(antes);
            String ultimoMonto = null;
            while (montos.find()) {
                ultimoMonto = montos.group();
            }
            if (ultimoMonto != null) {
                BigDecimal valor = parseMonto(ultimoMonto);
                if (valor != null) {
                    return valor;
                }
            }
        }
        advertencias.add("No se detectó el importe total — completar manualmente.");
        return null;
    }

    /** {neto, alicuota}: usa el desglose por alícuota si está (facturas A), si no "IVA Contenido" (facturas B/C), si no total=neto sin IVA. */
    private BigDecimal[] extraerNetoYAlicuota(String texto, BigDecimal total, List<String> advertencias) {
        Matcher neto = NETO_GRAVADO.matcher(texto);
        if (neto.find()) {
            BigDecimal netoGravado = parseMonto(neto.group(1));
            Matcher alicuotaM = IVA_ALICUOTA.matcher(texto);
            BigDecimal alicuotaDominante = BigDecimal.ZERO;
            while (alicuotaM.find()) {
                BigDecimal montoIva = parseMonto(alicuotaM.group(2));
                if (montoIva != null && montoIva.compareTo(BigDecimal.ZERO) > 0) {
                    alicuotaDominante = new BigDecimal(alicuotaM.group(1).replace(",", "."));
                }
            }
            if (netoGravado != null) {
                return new BigDecimal[]{netoGravado, alicuotaDominante};
            }
        }

        Matcher ivaContenido = IVA_CONTENIDO.matcher(texto);
        if (ivaContenido.find() && total != null) {
            BigDecimal iva = parseMonto(ivaContenido.group(1));
            if (iva != null && iva.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal netoCalculado = total.subtract(iva);
                BigDecimal alicuota = snapAlicuota(iva.multiply(new BigDecimal("100"))
                        .divide(netoCalculado, 4, RoundingMode.HALF_UP));
                return new BigDecimal[]{netoCalculado, alicuota};
            }
        }

        Matcher subtotalM = SUBTOTAL.matcher(texto);
        if (subtotalM.find() && total != null) {
            BigDecimal subtotal = parseMonto(subtotalM.group(1));
            if (subtotal != null && subtotal.compareTo(BigDecimal.ZERO) > 0) {
                if (subtotal.compareTo(total) < 0) {
                    BigDecimal iva = total.subtract(subtotal);
                    BigDecimal alicuota = snapAlicuota(iva.multiply(new BigDecimal("100"))
                            .divide(subtotal, 4, RoundingMode.HALF_UP));
                    return new BigDecimal[]{subtotal, alicuota};
                }
                return new BigDecimal[]{subtotal, BigDecimal.ZERO};
            }
        }

        if (total == null) {
            advertencias.add("No se pudo determinar el neto/IVA — completar manualmente.");
            return new BigDecimal[]{null, null};
        }
        advertencias.add("No se detectó desglose de IVA — se sugiere el importe total como neto (0% IVA); revisar y corregir.");
        return new BigDecimal[]{total, BigDecimal.ZERO};
    }

    private BigDecimal snapAlicuota(BigDecimal calculada) {
        return ALICUOTAS_CONOCIDAS.stream()
                .min((a, b) -> a.subtract(calculada).abs().compareTo(b.subtract(calculada).abs()))
                .orElse(BigDecimal.ZERO);
    }

    private static String extraerPrimero(Pattern patron, String texto) {
        Matcher m = patron.matcher(texto);
        return m.find() ? m.group(1) : null;
    }

    private static BigDecimal extraerDecimalPunto(Pattern patron, String texto) {
        Matcher m = patron.matcher(texto);
        if (!m.find()) {
            return null;
        }
        try {
            return new BigDecimal(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Acepta "1.450.000,00" (miles+decimal AR), "483,00" (decimal AR sin miles) y "15560.33" (decimal literal). */
    private static BigDecimal parseMonto(String valor) {
        String v = valor.trim();
        try {
            if (v.matches("\\d{1,3}(\\.\\d{3})+,\\d{2}")) {
                return new BigDecimal(v.replace(".", "").replace(",", "."));
            }
            if (v.matches("\\d+,\\d{2}")) {
                return new BigDecimal(v.replace(",", "."));
            }
            if (v.matches("\\d+\\.\\d{2}")) {
                return new BigDecimal(v);
            }
            if (v.matches("\\d+")) {
                return new BigDecimal(v);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    private static String normalizarCuit(String cuit) {
        String digitos = cuit.replaceAll("[^0-9]", "");
        if (digitos.length() != 11) {
            return cuit;
        }
        return digitos.substring(0, 2) + "-" + digitos.substring(2, 10) + "-" + digitos.substring(10);
    }
}
