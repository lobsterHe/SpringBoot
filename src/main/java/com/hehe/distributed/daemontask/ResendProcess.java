package com.hehe.distributed.daemontask;


import com.hehe.distributed.config.RabbitTemplateConfig;
import com.hehe.distributed.sender.RabbitSender;
import com.hehe.distributed.util.AlertSender;
import com.hehe.distributed.util.DBCoordinator;
import com.hehe.distributed.util.MQConstants;
import com.hehe.distributed.util.RabbitMetaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@EnableScheduling
public class ResendProcess {
    private Logger logger = LoggerFactory.getLogger(RabbitTemplateConfig.class);

    @Autowired
    DBCoordinator dbCoordinator;

    @Autowired
    RabbitSender rabbitSender;

    @Autowired
    AlertSender alertSender;

    /**
     * @Description: 1小时扫描一次
     * prepare状态的消息超时告警(业务执行失败)
     */
    @Scheduled(cron = "0 0 0/1 * * ?")
    public void alertPrepareMsg() throws Exception{
        List<String> messageIds = dbCoordinator.getMsgPrepare();
        for(String messageId: messageIds){
            alertSender.doSend(messageId);
        }
    }

    /**
     * @Description: 1小时扫描一次
     *
     */
    @Scheduled(cron = "0 0 0/1 * * ?")
    public void resendReadyMsg() throws Exception{
        List<RabbitMetaMessage> messages = dbCoordinator.getMsgReady();
        for(RabbitMetaMessage message: messages){
            long msgCount = dbCoordinator.getResendValue(MQConstants.MQ_RESEND_COUNTER,message.getMessageId());
            if( msgCount > MQConstants.MAX_RETRY_COUNT){
                alertSender.doSend(message.getMessageId());
            }
            rabbitSender.send(message);
            dbCoordinator.incrResendKey(MQConstants.MQ_RESEND_COUNTER, message.getMessageId());
        }
    }


}
