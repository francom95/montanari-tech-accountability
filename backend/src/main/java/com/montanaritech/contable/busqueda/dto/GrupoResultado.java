package com.montanaritech.contable.busqueda.dto;

import java.util.List;

public record GrupoResultado(List<ResultadoItem> items, long total) {}
