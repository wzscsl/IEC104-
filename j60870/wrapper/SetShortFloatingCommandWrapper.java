package com.openmuc.j60870.wrapper;

import com.openmuc.j60870.ASduType;
import com.openmuc.j60870.ie.IeQualifierOfSetPointCommand;
import com.openmuc.j60870.ie.IeShortFloat;
import com.openmuc.j60870.ie.InformationElement;
import com.openmuc.j60870.ie.InformationObject;

import java.util.Map;

/**
 * @ Author     ：dengzhihai
 * @ Date       ：Created in 14:17 2023/5/6
 * @ Description：
 * @ Modified By：
 * @Version:
 */
//设点命令，浮点数值-50-C_SE_NC_1
public class SetShortFloatingCommandWrapper extends PointWrapper{
    private IeShortFloat ieValue;
    private IeQualifierOfSetPointCommand ieQuality;
    private float srcValue;

    public SetShortFloatingCommandWrapper(Map<String,String> pointMap, int address, String name, ASduType aSduType,float deadLine){
        super(pointMap,address,name,aSduType,deadLine);
        ieValue = new IeShortFloat(0);
        ieQuality = new IeQualifierOfSetPointCommand(1, true);
        srcValue = 0f;
        informationObject = new InformationObject(address, new InformationElement[][]{{ieValue, ieQuality}});
    }
    //遥控点的更新是从ie点到外
    public void write(Object data){
        //ieValue = (IeShortFloat)data;
        float newValue = Float.parseFloat(data.toString());
        float diff = srcValue - newValue;
        if(Math.abs(diff)>deadLine){
            setChangeFlag();
        }
        ieValue.setValue(newValue);
        srcValue = newValue;
        ieQuality.setQl(0);
    }

    public void set(Object data,boolean invalid){
        Float newValue = Float.parseFloat(data.toString());
        if(srcValue != newValue){
            ieValue.setValue(newValue);
            setChangeFlag();
        }
        srcValue = newValue;
        ieQuality.setQl(0);
    }

    public Object get(){
        return srcValue;
    }

    public Number getNumber(){
        return srcValue;
    }
}
