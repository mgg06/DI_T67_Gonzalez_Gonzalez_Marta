package es.marta.gymfit;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;

public final class Validacion {
    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private Validacion() { }
    public static LocalDate fecha(String texto) { return LocalDate.parse(texto.trim(), FECHA); }
    public static String ibanNormalizado(String texto) { return texto.replaceAll("\\s", "").toUpperCase(Locale.ROOT); }
    public static boolean ibanValido(String texto) {
        String iban = ibanNormalizado(texto);
        if (iban.isEmpty()) return true;
        if (!iban.matches("ES[0-9]{22}")) return false;
        String movido = iban.substring(4) + iban.substring(0, 4);
        StringBuilder numeros = new StringBuilder();
        for (char c : movido.toCharArray()) numeros.append(Character.isDigit(c) ? String.valueOf(c) : String.valueOf(c - 'A' + 10));
        return new BigInteger(numeros.toString()).mod(BigInteger.valueOf(97)).intValue() == 1;
    }
}
