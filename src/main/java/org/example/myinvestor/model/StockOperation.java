package org.example.myinvestor.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class StockOperation {
    public final LocalDate fechaOperacion;
    public final LocalDate fechaValor;
    public final LocalDateTime fechaHoraEjecucion;
    public final String tipoOperacion;
    public final String mercado;
    public final String valorNombre;
    public final String isin;
    public final String referenciaOperacion;
    public final BigDecimal cantidad;
    public final Money precioBruto;
    public final Money importeBruto;
    public final Money comisiones;
    public final Money gastos;
    public final Money tasasImpuestos;
    public final Money retencionOrigen;
    public final Money retencionDestino;
    public final Money importeNeto;
    public final BigDecimal importeNetoEur;
    public final String emailSubject;
    public final LocalDateTime emailDate;

    public StockOperation(LocalDate fechaOperacion, LocalDate fechaValor, LocalDateTime fechaHoraEjecucion,
                           String tipoOperacion, String mercado, String valorNombre, String isin,
                           String referenciaOperacion, BigDecimal cantidad, Money precioBruto, Money importeBruto,
                           Money comisiones, Money gastos, Money tasasImpuestos, Money retencionOrigen,
                           Money retencionDestino, Money importeNeto, BigDecimal importeNetoEur,
                           String emailSubject, LocalDateTime emailDate) {
        this.fechaOperacion = fechaOperacion;
        this.fechaValor = fechaValor;
        this.fechaHoraEjecucion = fechaHoraEjecucion;
        this.tipoOperacion = tipoOperacion;
        this.mercado = mercado;
        this.valorNombre = valorNombre;
        this.isin = isin;
        this.referenciaOperacion = referenciaOperacion;
        this.cantidad = cantidad;
        this.precioBruto = precioBruto;
        this.importeBruto = importeBruto;
        this.comisiones = comisiones;
        this.gastos = gastos;
        this.tasasImpuestos = tasasImpuestos;
        this.retencionOrigen = retencionOrigen;
        this.retencionDestino = retencionDestino;
        this.importeNeto = importeNeto;
        this.importeNetoEur = importeNetoEur;
        this.emailSubject = emailSubject;
        this.emailDate = emailDate;
    }
}
