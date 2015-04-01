package org.votingsystem.test.util;

import org.h2.tools.DeleteDbFiles;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;

import java.io.ByteArrayInputStream;
import java.sql.*;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class H2Utils {

    private static Logger log = Logger.getLogger(H2Utils.class.getSimpleName());

    public static void testBlob() throws Exception {
        String url = ContextVS.APPDIR + "/testH2Blob";
        log.info("url: " + url);
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection("jdbc:h2:" + url);
        final long testLength = Integer.MAX_VALUE + 110L;
        Statement stat = conn.createStatement();
        stat.execute("set COMPRESS_LOB LZF");
        stat.execute("create table test(x blob)");
        PreparedStatement prep = conn.prepareStatement("insert into test values(?)");
        prep.setBinaryStream(1, new ByteArrayInputStream("---- Blob content ----".getBytes("UTF-8")));
        prep.executeUpdate();
        ResultSet rs = stat.executeQuery("select length(x) from test");
        rs.next();
        log.info("testLength - expected: " + testLength + " - stored: " + rs.getLong(1));
        rs = stat.executeQuery("select x from test");
        rs.next();
        byte[] storedBytes = FileUtils.getBytesFromStream(rs.getBinaryStream(1));
        log.info("storedBytes: " + new String(storedBytes));
        conn.close();
    }

    public static void testPlainText() throws Exception {
        String url = ContextVS.APPDIR + "/testH2PlainText";
        log.info("url: " + url);
        DeleteDbFiles.execute("~", url, true);
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection("jdbc:h2:" + url);
        Statement stat = conn.createStatement();
        stat.execute("create table test(id int primary key, name varchar(255))");
        stat.execute("insert into test values(1, 'Hello From H2 Database')");
        ResultSet rs;
        rs = stat.executeQuery("select * from test");
        while (rs.next()) {
            log.info(rs.getString("name"));
        }
        stat.close();
        conn.close();
    }
}
