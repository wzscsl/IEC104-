package com.openmuc.j60870;

import com.openmuc.j60870.ie.InformationElement;
import com.openmuc.j60870.ie.InformationObject;
import com.openmuc.j60870.wrapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @ Author     ：dengzhihai
 * @ Date       ：Created in 15:17 2023/4/25
 * @ Description：104数据存储区域
 * @ Modified By：
 * @Version:
 *
 * 常用点：
 *监视方向的过程信息
 * <1 > ： = 单点信息 M_SP_NA_1
 * <11> ：= 测量值 , 标度化值 M_ME_NB_1
 * <13> ：= 测量值 , 短浮点数 M_ME_NC_1
 *
 *在控制方向的过程信息
 * CON<45> ： = 单点命令 C_SC_NA_1
 * CON<49> ： = 设定值命令 , 标度化值 C_SE_NB_1
 * CON<50> ： = 设定值命令 , 短浮点数 C_SE_NC_1
 */
public class DataProcessImage {
    //公共地址
    private int slaveId = 1;
    //常用对象类型：装置状态、双点遥信、单点遥信、遥测、遥脉、步位置、遥控
    //PointWrapper 地址连续,这种分类方式方便总召时分类上传
    Map<ASduType,PointWrapper[]> asduTypeMap = new HashMap<>();
    //地址-点，这种映射方式方便更新每个地址的数据
    Map<Integer,PointWrapper> addressMap = new HashMap<>();
    //总召批量发送数据
    Map<ASduType,List<InformationObject>> sendBatchMap = new HashMap<>();

    //监听写操作
    private List<DataProcessListener> writeListeners = new ArrayList<>();
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DataProcessImage(int slaveId){
        this.slaveId = slaveId;
    }

    public void init(int slaveId,Map<String ,Map<String,String>> dataMap){
        this.slaveId = slaveId;
        initPointWrapper(dataMap);
        initSendBatchs();

    }

    public void init(int slaveId,List<PointWrapper>  pointList){
        this.slaveId = slaveId;
        initPointWrapper(pointList);
        initSendBatchs();

    }

    public Map<ASduType,List<InformationObject>> getSendBatchMap(){
        return  sendBatchMap;
    }

    //初始化每个点
    private void initPointWrapper(Map<String ,Map<String,String>> dataMap){
        if(dataMap == null || dataMap.size()<=0) return;
        Map<ASduType,List<PointWrapper>> asduTypeTempMap = new HashMap<>();
        Map<Integer,PointWrapper> addressTempMap = new HashMap<>();
        for(Map<String,String> pointMap : dataMap.values()){
            String[] dataTypeArray = pointMap.get("DataType").split(",");
            //String[] scaleIndexArray = pointMap.get("ScaleIndex").split(",");
            //String[] windmachineids = pointMap.get("windOrFarmId").split(",");
            //String variableTypeArray = pointMap.get("variableType");
            //String paramCode = pointMap.get("ParamCode");
            String[] addressArray = pointMap.get("RegisterAddress").split(",");
            String[] nameArray = pointMap.get("VariableName").split(",");
            String[] extraArray = pointMap.get("extra").split(",");
            for (int i = 0; i < addressArray.length; i++) {
                String dataType = dataTypeArray[i];
                String name = nameArray[i];
                int address = Integer.valueOf(addressArray[i]);
                float deadLine = 0;
                if(i<extraArray.length-1 && extraArray[i]!=null && !extraArray[i].equals("")) //当extra存储为空时，split后会少一个
                    deadLine = Float.valueOf(extraArray[i]);
                PointWrapper pointWrapper = null;
                ASduType asduType = null;
                if(dataType.equals("M_ME_NB_1")){
                    asduType = ASduType.M_ME_NB_1;
                    pointWrapper = new ScaledValuePointWrapper(pointMap,address,name,asduType,deadLine);
                }else if(dataType.equals("M_ME_NC_1")){
                    asduType = ASduType.M_ME_NC_1;
                    pointWrapper = new ShortFloatingPointWrapper(pointMap,address,name,asduType,deadLine);
                }else if(dataType.equals("M_SP_NA_1")){
                    asduType = ASduType.M_SP_NA_1;
                    pointWrapper = new SinglePointWithQualityWrapper(pointMap,address,name,asduType,deadLine);
                }else if(dataType.equals("M_SP_TB_1")){
                    asduType = ASduType.M_SP_TB_1;
                    pointWrapper = new SinglePointWithTimeWrapper(pointMap,address,name,asduType,deadLine);
                }else if(dataType.equals("C_SE_NC_1")){
                    asduType = ASduType.C_SE_NC_1;
                    pointWrapper = new SetShortFloatingCommandWrapper(pointMap,address,name,asduType,deadLine);
                }else if(dataType.equals("C_SC_NA_1")){
                    asduType = ASduType.C_SC_NA_1;
                    pointWrapper = new SingleCommandWrapper(pointMap,address,name,asduType,deadLine);
                }
                if(asduType != null){
                    if(!asduTypeTempMap.containsKey(asduType))
                        asduTypeTempMap.put(asduType,new ArrayList<>());
                    asduTypeTempMap.get(asduType).add(pointWrapper);
                    addressMap.put(address,pointWrapper);
                }
            }
        }
        for(ASduType aSduType : asduTypeTempMap.keySet()){
            List<PointWrapper> pointWrapperList = asduTypeTempMap.get(aSduType);
            int maxAddress = 0;
            int minAddress = pointWrapperList.get(0).getAddress();
            for(PointWrapper pointWrapper : pointWrapperList){
                if(maxAddress<pointWrapper.getAddress())
                    maxAddress = pointWrapper.getAddress();
                if(minAddress>pointWrapper.getAddress())
                    minAddress = pointWrapper.getAddress();
            }
            int size = maxAddress-minAddress+1;
            PointWrapper[] pointWrapperArray = new PointWrapper[size];
            for(PointWrapper pointWrapper : pointWrapperList){
                pointWrapperArray[pointWrapper.getAddress()-minAddress] = pointWrapper;
            }
            asduTypeMap.put(aSduType,pointWrapperArray);
        }
    }


    //初始化每个点,暂未实现
    private void initPointWrapper(List<PointWrapper> pointWrapperList){
        if(pointWrapperList == null || pointWrapperList.size()<=0) return;
        PointWrapper pointTemp = null;
        try{
            Map<ASduType,List<PointWrapper>> asduTypeTempMap = new HashMap<>();
            Map<Integer,PointWrapper> addressTempMap = new HashMap<>();
            for(PointWrapper point : pointWrapperList){
                pointTemp = point;
                if(!asduTypeTempMap.containsKey(point.getASduType()))
                    asduTypeTempMap.put(point.getASduType(),new ArrayList<>());
                asduTypeTempMap.get(point.getASduType()).add(point);
                addressMap.put(point.getAddress(),point);
            }
            for(ASduType aSduType : asduTypeTempMap.keySet()){
                List<PointWrapper> tempList = asduTypeTempMap.get(aSduType);
                tempList.sort(Comparator.comparing(obj -> obj.getAddress()));
                int maxAddress = tempList.get(tempList.size()-1).getAddress();
                int minAddress = tempList.get(0).getAddress();
                int size = maxAddress-minAddress+1;
                PointWrapper[] pointWrapperArray = new PointWrapper[size];
                for(PointWrapper pointWrapper : tempList){
                    pointWrapperArray[pointWrapper.getAddress()-minAddress] = pointWrapper;
                }
                asduTypeMap.put(aSduType,pointWrapperArray);
            }
        }catch (Exception ex){
            if(pointTemp != null) logger.error("地址"+pointTemp.getAddress() + "可能错误");
            throw ex;
        }

    }


    //初始化总召发送,或者自动上送的发送。地址连续上送
    private void initSendBatchs(){
        sendBatchMap = new HashMap<>();
        for(ASduType aSduType :  asduTypeMap.keySet()){
            if(aSduType==ASduType.C_SC_NA_1 || aSduType==ASduType.C_SE_NB_1|| aSduType==ASduType.C_SE_NC_1)
                continue;
            if(!sendBatchMap.containsKey(aSduType))
                sendBatchMap.put(aSduType,new ArrayList<>());
            List<PointWrapper[]> sendBatchs = new ArrayList<>();
            PointWrapper[] pointWrapperArray = asduTypeMap.get(aSduType);
            int maxSendLength = maxLength(aSduType);
            //临时变量存放
            List<PointWrapper> pointWrapperBatch = new ArrayList<>();
            //临时变量存放
            List<InformationElement[]> informationElementsTemp = new ArrayList<>();
            for(int i=0;i<pointWrapperArray.length;i++){
                PointWrapper pointWrapper = pointWrapperArray[i];
                if(pointWrapper != null){
                    pointWrapperBatch.add(pointWrapper);
                    pointWrapper.fillBuffer(informationElementsTemp);
                }
                if((pointWrapperBatch.size()>0 && pointWrapper == null) //若地址没有连续,则单独一次上送
                    || pointWrapperBatch.size()>maxSendLength  //若长度已经最大
                    || (i==pointWrapperArray.length-1) && pointWrapper != null && pointWrapperBatch.size()>0){ //已经是最后一个点
                    int startAddress = pointWrapperBatch.get(0).getAddress();
                    InformationElement[][] informationElements = informationElementsTemp.toArray(new InformationElement[informationElementsTemp.size()][]);
                    InformationObject informationObject = new InformationObject(startAddress,informationElements);
                    //每个pointWrapper会关联一个informationObject，用于变化后发送
                    pointWrapperBatch.forEach(pointWrapper1 -> pointWrapper1.setBatchInformationObject(informationObject));
                    sendBatchMap.get(aSduType).add(informationObject);
                    pointWrapperBatch = new ArrayList<>();
                    informationElementsTemp = new ArrayList<>();
                }
            }
        }
    }

    //程序内部更新对应地址的数据
    public void set(int address, Object data){
        set(address,data,false);
    }

    //程序内部更新对应地址的数据
    public void set(int address, Object data,boolean invalid){
        if(addressMap.containsKey(address))
            addressMap.get(address).set(data,invalid);
    }

    public void setInvalid(int address,boolean invalid) {
        if(addressMap.containsKey(address))
            addressMap.get(address).setInvalid(invalid);
    }

    public void setWithTimestamp(int address, Object data, long timestampMs) {
        setWithTimestamp(address, data, timestampMs, false);
    }

    public void setWithTimestamp(int address, Object data, long timestampMs, boolean invalid) {
        if (!addressMap.containsKey(address)) {
            return;
        }
        PointWrapper pointWrapper = addressMap.get(address);
        if (pointWrapper instanceof SinglePointWithTimeWrapper) {
            ((SinglePointWithTimeWrapper) pointWrapper).setWithTimestamp(data, timestampMs, invalid);
        } else {
            pointWrapper.set(data, invalid);
        }
    }

    public PointWrapper getPointWrapper(int address) {
        return addressMap.get(address);
    }

    //读取某个地址的数据
    public Object get(int address){
        if(addressMap.containsKey(address))
            return addressMap.get(address).get();
        return 0;
    }

    //读取某个地址的数据
    public Number getNumber(int address){
        if(addressMap.containsKey(address))
            return addressMap.get(address).getNumber();
        return 0;
    }

    //主站写入
    public void write(int address,Number value){
        if(addressMap.containsKey(address)){
            PointWrapper pointWrapper = addressMap.get(address);
            Number old = pointWrapper.getNumber();
            pointWrapper.write(value);
            for (DataProcessListener l : writeListeners)
                l.write(address, old, value);
        }
    }




    //获取每种类型每次发送的最大长度
    private int maxLength(ASduType aSduType){
        if(aSduType == ASduType.M_ME_NC_1)
            return 40;
        else if(aSduType == ASduType.M_SP_NA_1)
            return 100;
        else if(aSduType == ASduType.M_SP_TB_1)
            return 30;
        else
            return 40;

    }


    public synchronized void addListener(DataProcessListener l) {
        writeListeners.add(l);
    }

    public synchronized void removeListener(DataProcessListener l) {
        writeListeners.remove(l);
    }

    public synchronized void removeListener() {
        writeListeners.clear();
    }








}
