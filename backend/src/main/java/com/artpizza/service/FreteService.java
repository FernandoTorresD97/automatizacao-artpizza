package com.artpizza.service;

import com.artpizza.exception.RegraNegocioException;
import com.artpizza.model.RegiaoFrete;
import com.artpizza.repository.RegiaoFreteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class FreteService {

    private final RegiaoFreteRepository regiaoFreteRepository;

    public BigDecimal calcularFrete(String bairro) {
        RegiaoFrete regiao = regiaoFreteRepository.findByBairroIgnoreCaseAndAtivoTrue(bairro)
                .orElseThrow(() -> new RegraNegocioException(
                        "Ainda não entregamos no bairro '" + bairro + "'. Fale com o atendente para confirmar."));
        return regiao.getValorFrete();
    }
}
