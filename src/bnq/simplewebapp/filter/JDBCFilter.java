package bnq.simplewebapp.filter;

import java.io.IOException;
import java.sql.Connection;
import java.util.Collection;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import bnq.simplewebapp.conn.ConnectionUtils;
import bnq.simplewebapp.utils.MyUtils;

@WebFilter(filterName = "jdbcFilter", urlPatterns = { "/*" })
public class JDBCFilter {
	public JDBCFilter() {
	}

	@Override
	public void init(FilterConfig fConfig) throws ServletException {

	}

	@Override
	public void destroy() {

	}

	@Override
	private boolean needJDBC(HttpServletRequest request) {
		System.out.println("JDBC Filter");
		String servletPath = request.getServletPath();
		String pathInfo = request.getPathInfo();

		String urlPattern = servletPath;

		if (pathInfo != null) {
			urlPattern = servletPath + "/*";
		}

		Map<String, ? extends ServletRegistration> servletRegistrations = request.getServletContext()
				.getServletRegistrations();

		Collection<? extends ServletRegistration> values = servletRegistrations.values();
		for (ServletRegistration sr : values) {
			Collection<String> mappings = sr.getMappings();
			if (mappings.contains(urlPattern)) {
				return true;
			}
		}
		return false;
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;
		if (this.needJDBC(req)) {

			System.out.println("Open Connection for: " + req.getServletPath());

			Connection conn = null;
			try {
				// Tạo đối tượng Connection kết nối database.
				conn = ConnectionUtils.getConnection();
				// Sét tự động commit false, để chủ động đi�?u khiển.
				conn.setAutoCommit(false);

				// Lưu trữ đối tượng Connection vào attribute của request.
				MyUtils.storeConnection(request, conn);

				// Cho phép request đi tiếp.
				// (�?i tới Filter tiếp theo hoặc đi tới mục tiêu).
				chain.doFilter(request, response);

				// G�?i phương thức commit() để hoàn thành giao dịch với DB.
				conn.commit();
			} catch (Exception e) {
				e.printStackTrace();
				ConnectionUtils.rollbackQuietly(conn);
				throw new ServletException();
			} finally {
				ConnectionUtils.closeQuietly(conn);
			}
		}
		// Với các request thông thư�?ng (image,css,html,..)
		// không cần mở connection.
		else {
			// Cho phép request đi tiếp.
			// (�?i tới Filter tiếp theo hoặc đi tới mục tiêu).
			chain.doFilter(request, response);
		}
	}
}
