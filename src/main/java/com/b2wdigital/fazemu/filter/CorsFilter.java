package com.b2wdigital.fazemu.filter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

public interface CorsFilter {
	
	void doFilter(HttpServletRequest request, HttpServletResponse response) throws ServletException;

}
