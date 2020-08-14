package com._4paradigm.fesql_auto_test.performance;

import com._4paradigm.fesql_auto_test.util.FesqlUtil;
import com._4paradigm.sql.DataType;
import com._4paradigm.sql.ResultSet;
import com._4paradigm.sql.SQLRequestRow;
import com._4paradigm.sql.Schema;
import com._4paradigm.sql.sdk.SdkOption;
import com._4paradigm.sql.sdk.SqlExecutor;
import com._4paradigm.sql.sdk.impl.SqlClusterExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class FESQLMixerWorkloadExample extends BaseExample {
    private static final Logger logger = LoggerFactory.getLogger(RequestAggExample.class);
    private SqlExecutor executor;
    private SdkOption option;
    private String db = "dbbian";
    private String cardDdl = "create table card_info(\n" +
            "crd_nbr string,\n" +
            "clt_nbr string,\n" +
            "act_nbr string,\n" +
            "crd_atv_flg int,\n" +
            "cyc_day int,\n" +
            "crd_hdr_typ int,\n" +
            "chp_crd_flg int,\n" +
            "crd_pur_pin_flg int,\n" +
            "crd_csh_pin_flg int,\n" +
            "crd_exp_flg int,\n" +
            "crd_vip_drp int,\n" +
            "crd_isu_dte timestamp,\n" +
            "crd_exp_dte timestamp,\n" +
            "crd_lst_isu_dte timestamp,\n" +
            "crd_atv_dte timestamp,\n" +
            "index(key=crd_nbr, ts=crd_lst_isu_dte, ttl=2, ttl_type=latest));";


    private String tranDdl1 = "create table tran(\n" +
            "tran_no string,\n" +
            "card_no string,\n" +
            "trx_date date,\n" +
            "trx_time timestamp,\n" +
            "trx_amt double,\n" +
            "input_source int,\n" +
            "pos_entry_mode int,\n" +
            "acq_id int,\n" +
            "merchant_type int,\n" +
            "terminal int,\n" +
            "merchant_id int,\n" +
            "country_code int,\n" +
            "crdh_pre_ind int,\n" +
            "usd_amt double,\n" +
            "org_amt double,\n" +
            "org_curr_code int,\n" +
            "term_city int,\n" +
            "term_st int,\n" +
            "pin_check int,\n" +
            "tran_label int,\n" +
            "index(key=card_no, ts=trx_time, ttl=1d, ttl_type=absolute));";
    private boolean setupOk = false;
    private int recordSize = 10000;
    private String cardFormat = "insert into card_info values('%s', 'clt_nbr0'," +
            "'act_nbr0', 1, 2, 3, 4, 5, 6, 7, 8, 1596697833000l, 1596697833000l, %dl, 1596697833000l);";
    private String tranFormat = "insert into tran values('tran_noxx', '%s', '2020-10-10', %dl, 1.0, 1, 2,3,4,5,6,7,8, 1.0, 2.0, 1, 2, 3, 4,5);";
    private Random random = new Random(System.currentTimeMillis());
    private ScheduledExecutorService sched = Executors.newScheduledThreadPool(1);
    private long counter = 0;
    private String cardId = "cardId";

    public FESQLMixerWorkloadExample() {
    }

    public void print() {
        logger.info("ddl:\n{}", cardDdl);
        logger.info("ddl:\n{}", tranDdl1);
        logger.info("insert:\n{}", String.format(cardFormat, cardId, System.currentTimeMillis()));

        String key = String.valueOf(random.nextInt(100000));
        logger.info("insert:\n{}", String.format(tranFormat, key, System.currentTimeMillis()));
    }

    public void initExecutor() {
        SdkOption sdkOption = new SdkOption();
        sdkOption.setSessionTimeout(30000);
        sdkOption.setZkCluster(zkCluster);
        sdkOption.setZkPath(zkPath);
        sdkOption.setEnableDebug(true);
        this.option = sdkOption;
        try {
            executor = new SqlClusterExecutor(option);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setup() {
        executor.createDB(db);
        executor.executeDDL(db, cardDdl);
        setupOk = executor.executeDDL(db, tranDdl1);
        makeData();
       /* sched.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                makeData();
            }
        }, 0, 1, TimeUnit.MINUTES);*/
    }

    public void makeData() {
        String sql = String.format(cardFormat, cardId, System.currentTimeMillis());
        executor.executeInsert(db, sql);
        for (int i = 0; i < 100; i++) {
            sql = String.format(tranFormat, cardId, System.currentTimeMillis() - i);
            boolean ok = executor.executeInsert(db, sql);
            System.out.println(ok);
        }
        String key = String.valueOf(random.nextInt(100000));
        executor.executeInsert(db, sql);
        for (int i = 0; i < 1000; i++) {
            sql = String.format(tranFormat, key, System.currentTimeMillis() - i);
            executor.executeInsert(db, sql);
        }
    }

    public void lastJoinBm() {
        String sql = "select * from\n" +
                "(select\n" +
                "card_no,\n" +
                "trx_time,\n" +
                "merchant_id,\n" +
//                "month(trx_time) as fea_month,\n" +
//                "dayofmonth(trx_time) as fea_month,\n" +
//                "hour(trx_time) as fea_hour,\n" +
//                "week(trx_time) as fea_week,\n" +
//                "substr(card_no, 1, 6) as card_no_prefix,\n" +
//                "max(trx_amt) over w30d as w30d_trx_max ,\n" +
//                "min(trx_amt) over w30d as w30d_trx_min,\n" +
//                "sum(trx_amt) over w30d,\n" +
//                "avg(trx_amt) over w30d,\n" +
//                "max(usd_amt) over w30d,\n" +
//                "min(usd_amt) over w30d,\n" +
//                "sum(usd_amt) over w30d,\n" +
//                "avg(usd_amt) over w30d,\n" +
//                "max(org_amt) over w30d,\n" +
//                "min(org_amt) over w30d,\n" +
//                "sum(org_amt) over w30d,\n" +
//                "avg(org_amt) over w30d,\n" +
//                "distinct_count(merchant_id) over w30d,\n" +
//                "count(merchant_id) over w30d,\n" +
//                "distinct_count(term_city) over w30d,\n" +
//                "count(term_city) over w30d,\n" +
//                "max(trx_amt) over w10d,\n" +
//                "min(trx_amt) over w10d,\n" +
//                "sum(trx_amt) over w10d,\n" +
//                "avg(trx_amt) over w10d,\n" +
//                "max(usd_amt) over w10d,\n" +
//                "min(usd_amt) over w10d,\n" +
//                "sum(usd_amt) over w10d,\n" +
//                "avg(usd_amt) over w10d,\n" +
//                "max(org_amt) over w10d,\n" +
//                "min(org_amt) over w10d,\n" +
//                "sum(org_amt) over w10d,\n" +
//                "avg(org_amt) over w10d,\n" +
                "distinct_count(merchant_id) over w10d,\n" +
                "count(merchant_id)  over w10d,\n" +
                "distinct_count(term_city)  over w10d,\n" +
                "count(term_city) over w10d\n" +
                "from  tran\n" +
                "window w30d as (PARTITION BY tran.card_no ORDER BY tran.trx_time ROWS_RANGE BETWEEN 30d PRECEDING AND CURRENT ROW),\n" +
                "w10d as (PARTITION BY tran.card_no ORDER BY tran.trx_time ROWS_RANGE BETWEEN 10d PRECEDING AND CURRENT ROW)) as trx_fe\n" +
                "last join card_info order by card_info.crd_lst_isu_dte on trx_fe.card_no = card_info.crd_nbr and trx_fe.trx_time >= card_info.crd_lst_isu_dte;\n";
        String key = cardId;
        String trans = "trans";
        SQLRequestRow row = executor.getRequestRow(db, sql);
        row.Init(key.length());
        row.AppendNULL();
        row.AppendString(key);
        row.AppendNULL();
        row.AppendTimestamp(System.currentTimeMillis());
        row.AppendDouble(10.0);
        row.AppendInt32(1);
        row.AppendInt32(2);
        row.AppendInt32(3);
        row.AppendInt32(4);
        row.AppendInt32(5);
        row.AppendInt32(6);
        row.AppendInt32(7);
        row.AppendInt32(8);
        row.AppendDouble(1.0);
        row.AppendDouble(2.0);
        row.AppendInt32(1);
        row.AppendInt32(2);
        row.AppendInt32(3);
        row.AppendInt32(4);
        row.AppendInt32(5);
        row.Build();
        ResultSet rs = executor.executeSQL(db, sql, row);
        row.delete();
        if (rs == null) {
            System.out.println("no result");
        } else {
            System.out.println("ok");
        }
        rs.Next();
        Schema schema = rs.GetSchema();
        for (int i = 0; i < schema.GetColumnCnt(); i++) {
            System.out.println(schema.GetColumnName(i));
            if (rs.IsNULL(i)) {
                System.out.println("null");
                continue;
            }
            DataType type = schema.GetColumnType(i);
            if (type.swigValue() == DataType.kTypeInt16.swigValue()) {
                System.out.println(rs.GetInt16Unsafe(i));
            } else if (type.swigValue() == DataType.kTypeInt32.swigValue()) {
                System.out.println(rs.GetInt32Unsafe(i));
            } else if (type.swigValue() == DataType.kTypeInt64.swigValue()) {
                System.out.println(rs.GetInt64Unsafe(i));
            } else if (type.swigValue() == DataType.kTypeFloat.swigValue()) {
                System.out.println(rs.GetFloatUnsafe(i));
            } else if (type.swigValue() == DataType.kTypeDouble.swigValue()) {
                System.out.println(rs.GetDoubleUnsafe(i));
            } else if (type.swigValue() == DataType.kTypeString.swigValue()) {
                System.out.println(rs.GetStringUnsafe(i));
            } else if (type.swigValue() == DataType.kTypeTimestamp.swigValue()) {
                System.out.println(rs.GetTimeUnsafe(i));
            }
        }
        FesqlUtil.show(rs);
        schema.delete();
        rs.delete();
    }

    public static void run() {
        final FESQLMixerWorkloadExample example = new FESQLMixerWorkloadExample();
        example.initExecutor();
        example.setup();
        example.lastJoinBm();
    }


}