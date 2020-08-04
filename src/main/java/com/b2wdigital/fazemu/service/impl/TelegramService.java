package com.b2wdigital.fazemu.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;

@Service
public class TelegramService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TelegramService.class);

	@Value("${fazemu.telegram.bot.token}")
	private String BOT_TOKEN;

    @Value("${fazemu.telegram.alerts.chatId}")
    private Long CHAT_ID;

	private static final String TITLE = "FAZEMU - ALERTA:";
	private static final String LINE_BREAK = "\n";
	private static final String BOLD = "*";
	
	public void sendMessage(String mensagem) {
		TelegramBot bot = new TelegramBot(BOT_TOKEN);

		SendMessage request = new SendMessage(CHAT_ID, mensagem)
				.parseMode(ParseMode.Markdown);

		SendResponse response = bot.execute(request);
		
		LOGGER.info("Telegram message sent {}. Response: {}", mensagem, response.message());
	}

	public String composeContingenciaMessage(String tipoContingencia, String estado) {
		StringBuilder sb = new StringBuilder()
				.append(BOLD + TITLE + BOLD)
				.append(LINE_BREAK)
				.append("Entrando em modo ")
				.append(BOLD + tipoContingencia + BOLD)
				.append(" para o estado ")
				.append(BOLD + estado + BOLD);
		
		return sb.toString();
	}
	
}
