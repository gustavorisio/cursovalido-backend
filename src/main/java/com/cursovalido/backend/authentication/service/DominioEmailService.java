package com.cursovalido.backend.authentication.service;

import java.util.Hashtable;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;
import org.springframework.stereotype.Service;

@Service
public class DominioEmailService {
    public boolean aceitaEmail(String dominio) throws NamingException {
        InitialDirContext contexto = criarContexto();
        try {
            return consultar(contexto, dominio);
        } catch (NameNotFoundException excecao) {
            return false;
        } finally {
            contexto.close();
        }
    }

    InitialDirContext criarContexto() throws NamingException {
        Hashtable<String, String> ambiente = new Hashtable<>();
        ambiente.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        ambiente.put("com.sun.jndi.dns.timeout.initial", "2000");
        ambiente.put("com.sun.jndi.dns.timeout.retries", "1");
        return new InitialDirContext(ambiente);
    }

    private boolean possuiRegistros(Attribute registro) {
        if (registro == null) {
            return false;
        }
        return registro.size() > 0;
    }

    private boolean consultar(InitialDirContext contexto, String dominio) throws NamingException {
        // O provedor JNDI transforma varios tipos em uma consulta DNS ANY.
        // Resolvedores podem responder ANY sem registros mesmo para dominios validos.
        var atributos = contexto.getAttributes(dominio, new String[] { "MX" });
        Attribute mx = atributos.get("MX");
        if (possuiRegistros(mx)) {
            for (int i = 0; i < mx.size(); i++) {
                String[] partes = mx.get(i).toString().trim().split("\\s+");
                if (partes.length != 2) {
                    continue;
                }
                if (!".".equals(partes[1])) {
                    return true;
                }
            }
            return false; // Null MX: o dominio declara que nao recebe e-mails.
        }
        // Na ausencia de MX, SMTP permite entrega ao endereco do proprio dominio.
        Attribute ipv4 = contexto.getAttributes(dominio, new String[] { "A" }).get("A");
        if (possuiRegistros(ipv4)) return true;
        Attribute ipv6 = contexto.getAttributes(dominio, new String[] { "AAAA" }).get("AAAA");
        return possuiRegistros(ipv6);
    }
}
