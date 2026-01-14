package cn_order_meal.utils;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;

public class JDBCUtils {

   private static final String url;
   private static final String username;
   private static final String password;

    static {
        try {
            //0.读取配置文件的配置信息
            InputStream in = JDBCUtils.class.getClassLoader().getResourceAsStream("jdbc.properties");
            Properties properties = new Properties();
            properties.load(in);
            //1.注册驱动
            Class.forName(properties.getProperty("driverClassName"));
            url = properties.getProperty("url");
            username = properties.getProperty("username");
            password = properties.getProperty("password");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 这个方法针对的是DML语句
    @SuppressWarnings("SqlSourceToSinkFlow")
    public static int executeUpdate(String sql, Object... args) throws Exception {
        //2.建立连接
        Connection connection = DriverManager.getConnection(url, username, password);
        //3.编写sql语句
        PreparedStatement pStmt = connection.prepareStatement(sql);
        //4.得到预编译对象
        //5.对sql语句的?赋值
        for (int i = 0; i < args.length; i++) {
            pStmt.setObject(i + 1, args[i]);
        }
        //6.执行sql得到结果
        int row = pStmt.executeUpdate();

        //7.释放资源
        pStmt.close();
        connection.close();
        return row;
    }
    @SuppressWarnings("SqlSourceToSinkFlow")
    public static int R_executeUpdate(Connection conn, String sql, Object... args) throws Exception {
        //3.编写sql语句
        PreparedStatement pStmt = conn.prepareStatement(sql);
        //4.得到预编译对象
        //5.对sql语句的?赋值
        for (int i = 0; i < args.length; i++) {
            pStmt.setObject(i + 1, args[i]);
        }
        //6.执行sql得到结果
        int row = pStmt.executeUpdate();

        //7.释放资源
        pStmt.close();
        return row;
    }
    @SuppressWarnings("SqlSourceToSinkFlow")
    public static <R> R executeQuerySingle(String sql, Class<R> clazz, Object... args) throws Exception {
        // 1. 获取链接try-with-resources语句来自动关闭资源
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            // 2. 预编译sql, 对?号赋值
            try (PreparedStatement pStmt = connection.prepareStatement(sql)) {
                for (int i = 0; i < args.length; i++) {
                    pStmt.setObject(i + 1, args[i]);
                }
                // 3. 执行sql语句得到结果
                try (ResultSet rs = pStmt.executeQuery()) {
                    R instance = null;
                    if (rs.next()) {
                        instance = clazz.getDeclaredConstructor().newInstance(); // 创建T的实例
                        Field[] fields = clazz.getDeclaredFields();
                        for (Field field : fields) {
                            // 暴力反射
                            field.setAccessible(true);
                            String fieldName = field.getName();
                            Object value = rs.getObject(fieldName);
                            field.set(instance, value);
                        }
                    }
                    return instance;
                }
            }
        }
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    public static <T> ArrayList<T> executeQueryGroup(String sql, Class<T> clazz, Object... args) throws Exception {
        Connection connection = DriverManager.getConnection(url, username, password);
        PreparedStatement pStmt = connection.prepareStatement(sql);
        for (int i = 0; i < args.length; i++) {
            pStmt.setObject(i + 1, args[i]);
        }
        ResultSet rs = pStmt.executeQuery();
        Field[] fields = clazz.getDeclaredFields();
        ArrayList<T> list = new ArrayList<>();
        while (rs.next()) {
            T instance = clazz.getDeclaredConstructor().newInstance(); // 使用反射创建实例
            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();
                Object value = rs.getObject(fieldName);
                field.set(instance, value);
            }
            list.add(instance);
        }
        rs.close();
        pStmt.close();
        connection.close();
        return list;
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    public static int executeQueryForCount(String sql) throws SQLException {
        // 1. 获取连接，try-with-resources语句来自动关闭资源
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            // 2. 预编译sql
            try (PreparedStatement pStmt = connection.prepareStatement(sql)) {
                // 3. 执行sql语句得到结果
                try (ResultSet rs = pStmt.executeQuery()) {
                    if (rs.next()) {
                        // 获取count(*)的结果并返回int值
                        return rs.getInt(1);
                    }
                }
            }
        }
        // 如果没有结果，则返回0或者抛出异常
        return 0;
    }
}
