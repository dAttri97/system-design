package com.attri.systemdesign.lld.pubsubsystem.subscriber;


import com.attri.systemdesign.lld.pubsubsystem.entities.Message;

public interface Subscriber {
    String getId();
    void onMessage(Message message);
}
