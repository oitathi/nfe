package com.b2wdigital.fazemu.presentation.web.service.rest;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HostInfosRest {

	@GetMapping(value = "/rest/host-infos")
	public String getHostInfos() {

		StringBuilder sb = new StringBuilder();
		InetAddress ip;
		String hostname;
		try {
			ip = InetAddress.getLocalHost();
			hostname = ip.getHostName();
			sb.append("Your current IP address : " + ip);
			sb.append("\n");
			sb.append("Your current Hostname : " + hostname);

		} catch (UnknownHostException e) {
			sb.append("ERROR: ").append(e.getMessage());
		}

		return sb.toString();
	}

}
