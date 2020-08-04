package com.b2wdigital.fazemu.presentation.web.controller;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.b2wdigital.fazemu.business.service.DocumentoEpecService;
import com.b2wdigital.fazemu.domain.form.DocumentoEpecForm;
import com.b2wdigital.fazemu.utils.DateUtils;

@CrossOrigin(origins = { "http://localhost:3000" })
@RestController
@RequestMapping(value = "/fazemu-web", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class DocumentosEpecController {

	private static final String DEFAULT_MAPPING = "/documentosEpec";

	private Locale locale = LocaleContextHolder.getLocale();
 
	@Autowired
	private MessageSource ms;

	@Autowired
	private DocumentoEpecService documentoEpecService;
	
	private Map<String, String> parameters;

	@GetMapping(value = DEFAULT_MAPPING)
	public List<DocumentoEpecForm> listByFiltros(@RequestParam Map<String, String> parameters) throws Exception {
		this.parameters = parameters;
		verifyIfDatesFieldsAreCorrectlyFilled();
		setRightFormatDatesInParametersMap();
		return documentoEpecService.listByFiltros(this.parameters);
	}

	private void verifyIfDatesFieldsAreCorrectlyFilled() throws Exception {
		String dataInicio = parameters.get("dataHoraRegInicio");
		String dataFim = parameters.get("dataHoraRegFim");

		if (StringUtils.isBlank(dataInicio) && StringUtils.isBlank(dataFim)) {
			return;
		}
		if (StringUtils.isNotBlank(dataInicio) && StringUtils.isNotBlank(dataFim)) {
			if (!isStartDateBeforeEndDate(dataInicio, dataFim)) {
				throw new Exception(ms.getMessage("error.dates.fields.interval", null, locale));
			}
			return;
		}
	}

	private boolean isStartDateBeforeEndDate(String dataInicio, String dataFim) throws Exception {
		try {
			Date dataInicioDt = DateUtils.convertStringToDate(dataInicio, "yyyy-MM-dd'T'HH:mm");
			Date dataFimDt = DateUtils.convertStringToDate(dataFim, "yyyy-MM-dd'T'HH:mm");
			return DateUtils.isStartDateBeforeEndDate(dataInicioDt, dataFimDt);
		} catch (Exception e) {
			throw new Exception(ms.getMessage("error.dates.fileds.parse", null, locale));
		}
	}
	
	private void setRightFormatDatesInParametersMap() throws ParseException {
		String startDateWrongFormat = this.parameters.get("dataHoraRegInicio");
		String endDateWrongFormat = this.parameters.get("dataHoraRegFim");
		
		if(StringUtils.isNotBlank(startDateWrongFormat)) {
			Date startDateWrongFormatDt = DateUtils.convertStringToDate(startDateWrongFormat, "yyyy-MM-dd'T'HH:mm");
			String startDateRightFormat = DateUtils.convertDateToString(startDateWrongFormatDt);
			this.parameters.put("dataHoraRegInicio",startDateRightFormat );
		}
		
		if (StringUtils.isNoneBlank(endDateWrongFormat)){
			Date endDateWrongFormatDt = DateUtils.convertStringToDate(endDateWrongFormat, "yyyy-MM-dd'T'HH:mm");
			String endDateRightFormat = DateUtils.convertDateToString(endDateWrongFormatDt);
			this.parameters.put("dataHoraRegFim",endDateRightFormat );
		}
	}

}
