package weaver.interfaces.schedule;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.wps.yun.StringUtil;
import weaver.conn.RecordSet;
import weaver.integration.logging.Logger;
import weaver.integration.logging.LoggerFactory;

/**
 * 
 * @author youcheng wu ponyai
 * 
 */
public class ChinaMoneyRateSyncJob extends BaseCronJob {

    public ChinaMoneyRateSyncJob() {
        logger = LoggerFactory.getLogger(getClass());
        fmt1 = new SimpleDateFormat("yyyy-MM-dd");
        fmt2 = new SimpleDateFormat("HH:mm:ss");
        insertSql = "insert into uf_china_rate(cxrq,bz,hl,formmodeid,modedatacreater,modedatacreatertype,modedatacreatedate,modedatacreatetime,modeuuid) values(?,?,?,2,22,0,?,?,?)";
        head = "https://www.chinamoney.com.cn/ags/ms/cm-u-bk-ccpr/CcprHisNew?pageNum=1&pageSize=100&startDate=";
    }
 

    public void execute() {
        //时间
        curDateStr = fmt1.format(new Date());
        curTimeStr = fmt2.format(new Date());
        long l = System.currentTimeMillis();
        logger.info((new StringBuilder()).append("ChinaMoneyRateSyncJob>>>>>>>>(")
                .append(getClass().getName()).append(")  start").toString());
        try {
            String[] currencys= {"USD/CNY","HKD/CNY","100JPY/CNY"};
            for (String currency : currencys) {
                saveData(currency);
            }
        } catch (Exception e) {
            logger.error("ChinaMoneyRateSyncJob execute error:",e);
        } 
        logger.info((new StringBuilder()).append(
                "ChinaMoneyRateSyncJob >>>>>>>>(")
                .append(getClass().getName()).append(")  end ")
                .append(System.currentTimeMillis() - l).toString());
    }
    
    private void saveData(String currency) throws Exception {
        String usdResult =callHttpGet(currency);
        if(StringUtil.isEmpty(usdResult)) {
            logger.error("chinarate interface return null");
            return;
        }
        JSONObject resultJson=JSONObject.parseObject(usdResult);
        String records= resultJson.getString("records");
        if(StringUtil.isEmpty(records)) {
            logger.error("chinarate interface return records null");
            return;
        }
        JSONArray recordArray=JSONObject.parseArray(records);
        JSONObject recordJson=recordArray.getJSONObject(0);
        String values=recordJson.getString("values");
        JSONArray valueArray=JSONObject.parseArray(values);
        BigDecimal rate=valueArray.getBigDecimal(0);
        if(null==rate) {
            logger.error("chinarate interface return rate null");
            return;
        }
        RecordSet recordset = new RecordSet();
        String uuid = UUID.randomUUID().toString();
        Boolean successFlag=recordset.executeUpdate(insertSql,curDateStr,currency,rate,curDateStr,curTimeStr,uuid);
        logger.info("save chinamoney_rate data end"+successFlag);
    }
    
   
    private  String callHttpGet (String currency) throws Exception {
        StringBuilder sb = new StringBuilder();
        String endDatestr = "&endDate=" + curDateStr;
        String curStr="&currency="+currency;
        sb.append(head).append(curDateStr).append(endDatestr).append(curStr);
        URL url = new URL(sb.toString());
        HttpURLConnection connect = (HttpURLConnection) url.openConnection();
        connect.setRequestMethod("GET");
        connect.connect();
        // connect.get
        InputStream is = connect.getInputStream();
        BufferedReader br   = new BufferedReader(new InputStreamReader(is));
        String str = "";
        String  result="";
        while ((str = br.readLine()) != null){
            result += str;
        }
        logger.info("chinarate interface callresult:\n"+result);
        //关闭流
        is.close();
        //断开连接，disconnect是在底层tcp socket链接空闲时才切断，如果正在被其他线程使用就不切断。
        connect.disconnect();
        return result;
    }

    private Logger logger;
    private String insertSql;
    SimpleDateFormat fmt1;
    SimpleDateFormat fmt2;
    private String curDateStr;
    private String curTimeStr;
    private String head;
}
