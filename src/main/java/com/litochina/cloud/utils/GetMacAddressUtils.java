package com.litochina.cloud.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class GetMacAddressUtils {

    private static final String[] windowsCommand = { "ipconfig", "/all" };
    private static final String[] linuxCommand = { "ifconfig", "-a" };
    private static final Pattern macPattern = Pattern.compile(".*((:?[0-9a-f]{2}[-:]){5}[0-9a-f]{2}).*",
            Pattern.CASE_INSENSITIVE);

    /**
       * 获取多个网卡地址
     * @return
     * @throws IOException
     */
    public static List<String> getMacAddressList() throws IOException {
        final ArrayList<String> macAddressList = new ArrayList<String>();
        final String os = System.getProperty("os.name");
        final String command[];

        if (os.startsWith("Windows")) {
            command = windowsCommand;
        } else if (os.startsWith("Linux")) {
            command = linuxCommand;
        } else {
            throw new IOException("Unknow operating system:" + os);
        }
        final Process process = Runtime.getRuntime().exec(command);// 执行命令
        BufferedReader bufReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = bufReader.readLine()) != null) {
            Matcher matcher = macPattern.matcher(line);
            if (matcher.matches()) {
                macAddressList.add(matcher.group(1).trim());
            }
        }
        process.destroy();
        bufReader.close();
        log.info("mac address list ==== {}",macAddressList.toString());
        return macAddressList;
    }

    public static void main(String[] args) throws IOException {
    	List<String> macAddressList = GetMacAddressUtils.getMacAddressList();
    	System.out.println(macAddressList.toString());
    }
}
