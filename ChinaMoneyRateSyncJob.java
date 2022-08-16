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

// Referenced classes of package weaver.interfaces.schedule:
// BaseCronJob
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
        curDateStr = fmt1.format(new Date());
        curTimeStr = fmt2.format(new Date());
        insertSql = "insert into uf_china_rate(cxrq,bz,hl,formmodeid,modedatacreater,modedatacreatertype,modedatacreatedate,modedatacreatetime,modeuuid) values(?,?,?,2,22,0,?,?,?)";
        head = "https://www.chinamoney.com.cn/ags/ms/cm-u-bk-ccpr/CcprHisNew?pageNum=1&pageSize=100&startDate=";
    }
 
    public static void main(String[] args) {
        //
    }

    public void execute() {
        long l = System.currentTimeMillis();
        logger.info((new StringBuilder()).append("同步汇率定时任务开始>>>>>>>>(")
                .append(getClass().getName()).append(")  start").toString());
        try {
            String[] currencys= {"USD/CNY","HKD/CNY","100JPY/CNY"};
            for (String currency : currencys) {
                saveData(currency);
            }
        } catch (Exception e) {
            logger.error("同步汇率定时任务发生非预期异常",e);
        } 
        logger.info((new StringBuilder()).append(
                "同步汇率定时任务结束>>>>>>>>(")
                .append(getClass().getName()).append(")  end ")
                .append(System.currentTimeMillis() - l).toString());
    }
    
    private void saveData(String currency) throws Exception {
        String usdResult =callHttpGet(currency);
        if(StringUtil.isEmpty(usdResult)) {
            logger.info("调用汇率查询接口返回结果为空");
            return;
        }
        JSONObject resultJson=JSONObject.parseObject(usdResult);
        String records= resultJson.getString("records");
        if(StringUtil.isEmpty(records)) {
            logger.info("费率查询结果为空");
            return;
        }
        JSONArray recordArray=JSONObject.parseArray(records);
        JSONObject recordJson=recordArray.getJSONObject(0);
        String values=recordJson.getString("values");
        JSONArray valueArray=JSONObject.parseArray(values);
        BigDecimal rate=valueArray.getBigDecimal(0);
        if(null==rate) {
            logger.info("费率为空");
            return;
        }
        System.out.println("今日"+currency+"汇率为"+rate);
        RecordSet recordset = new RecordSet();
        String uuid = UUID.randomUUID().toString();
        Boolean successFlag=recordset.executeUpdate(insertSql,curDateStr,currency,rate,curDateStr,curTimeStr,uuid);
        logger.info("保存chinamoney_rate表汇率数据结束"+successFlag);
    }
    
   
    private  String callHttpGet (String currency) throws Exception {
        StringBuilder sb = new StringBuilder();
        String endDatestr = "&endDate=" + curDateStr;
        String curStr="&currency="+currency;
        sb.append(head).append(curDateStr).append(endDatestr).append(curStr);
        URL url = new URL(sb.toString());
        logger.info("汇率查询URL:\n"+sb.toString());
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
        logger.info("汇率查询返回结果:\n"+result);
        
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
