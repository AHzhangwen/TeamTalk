package com.mogujie.tt.ui.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mogujie.tt.config.DBConstant;
import com.mogujie.tt.DB.entity.MessageEntity;
import com.mogujie.tt.DB.entity.UserEntity;
import com.mogujie.tt.R;
import com.mogujie.tt.config.MessageConstant;
import com.mogujie.tt.ui.helper.AudioPlayerHandler;
import com.mogujie.tt.config.IntentConstant;
import com.mogujie.tt.imservice.entity.AudioMessage;
import com.mogujie.tt.imservice.entity.ImageMessage;
import com.mogujie.tt.imservice.entity.MixMessage;
import com.mogujie.tt.imservice.entity.TextMessage;
import com.mogujie.tt.imservice.service.IMService;
import com.mogujie.tt.ui.activity.PreviewGifActivity;
import com.mogujie.tt.ui.activity.PreviewMessageImagesActivity;
import com.mogujie.tt.ui.activity.PreviewTextActivity;
import com.mogujie.tt.ui.helper.Emoparser;
import com.mogujie.tt.ui.widget.GifView;
import com.mogujie.tt.ui.widget.message.GifImageRenderView;
import com.mogujie.tt.utils.CommonUtil;
import com.mogujie.tt.utils.DateUtil;
import com.mogujie.tt.utils.FileUtil;
import com.mogujie.tt.utils.Logger;
import com.mogujie.tt.ui.widget.SpeekerToast;
import com.mogujie.tt.ui.widget.message.AudioRenderView;
import com.mogujie.tt.ui.widget.message.EmojiRenderView;
import com.mogujie.tt.ui.widget.message.ImageRenderView;
import com.mogujie.tt.ui.widget.message.MessageOperatePopup;
import com.mogujie.tt.ui.widget.message.RenderType;
import com.mogujie.tt.ui.widget.message.TextRenderView;
import com.mogujie.tt.ui.widget.message.TimeRenderView;
import com.mogujie.tt.ui.helper.listener.OnDoubleClickListener;
import com.mogujie.tt.utils.ScreenUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author : yingmu on 15-1-8.
 * @email : yingmu@mogujie.com.
 */
public class MessageAdapter extends BaseAdapter {
    private Logger logger = Logger.getLogger(MessageAdapter.class);

    private ArrayList<Object> msgObjectList = new ArrayList<>();

    /**
     * ????????????
     */
    private MessageOperatePopup currentPop;
    private Context ctx;
    /**
     * ????????????session?????????
     */
    private UserEntity loginUser;
    private IMService imService;

    public MessageAdapter(Context ctx) {
        this.ctx = ctx;
    }

    /**
     * ----------------------init ?????????????????????-----------------
     */

    public void setImService(IMService imService,UserEntity loginUser) {
        this.imService = imService;
        this.loginUser = loginUser;
    }

    /**
     * ----------------------??????????????????-----------------
     */
    public void addItem(final MessageEntity msg) {
        if (msg.getDisplayType() == DBConstant.MSG_TYPE_SINGLE_TEXT) {
            if (isMsgGif(msg)) {
                msg.setGIfEmo(true);
            } else {
                msg.setGIfEmo(false);
            }
        }
        int nextTime = msg.getCreated();
        if (getCount() > 0) {
            Object object = msgObjectList.get(getCount() - 1);
            if (object instanceof MessageEntity) {
                int preTime = ((MessageEntity) object).getCreated();
                boolean needTime = DateUtil.needDisplayTime(preTime, nextTime);
                if (needTime) {
                    Integer in = nextTime;
                    msgObjectList.add(in);
                }
            }
        } else {
            Integer in = msg.getCreated();
            msgObjectList.add(in);
        }
        /**???????????????*/
        if (msg.getDisplayType() == DBConstant.SHOW_MIX_TEXT) {
            MixMessage mixMessage = (MixMessage) msg;
            msgObjectList.addAll(mixMessage.getMsgList());
        } else {
            msgObjectList.add(msg);
        }
        if (msg instanceof ImageMessage) {
            ImageMessage.addToImageMessageList((ImageMessage) msg);
        }
        logger.d("#messageAdapter#addItem");
        notifyDataSetChanged();

    }

    private boolean isMsgGif(MessageEntity msg) {
        String content = msg.getContent();
        // @YM ????????????  ????????????????????????????????????????????????
        if (TextUtils.isEmpty(content)
                || !(content.startsWith("[") && content.endsWith("]"))) {
            return false;
        }
        return Emoparser.getInstance(this.ctx).isMessageGif(msg.getContent());
    }

    public MessageEntity getTopMsgEntity() {
        if (msgObjectList.size() <= 0) {
            return null;
        }
        for (Object result : msgObjectList) {
            if (result instanceof MessageEntity) {
                return (MessageEntity) result;
            }
        }
        return null;
    }

    public static class MessageTimeComparator implements Comparator<MessageEntity> {
        @Override
        public int compare(MessageEntity lhs, MessageEntity rhs) {
            if (lhs.getCreated() == rhs.getCreated()) {
                return lhs.getMsgId() - rhs.getMsgId();
            }
            return lhs.getCreated() - rhs.getCreated();
        }
    }

    ;

    /**
     * ????????????????????????,????????????????????????
     */
    public void loadHistoryList(final List<MessageEntity> historyList) {
        logger.d("#messageAdapter#loadHistoryList");
        if (null == historyList || historyList.size() <= 0) {
            return;
        }
        Collections.sort(historyList, new MessageTimeComparator());
        ArrayList<Object> chatList = new ArrayList<>();
        int preTime = 0;
        int nextTime = 0;
        for (MessageEntity msg : historyList) {
            if (msg.getDisplayType() == DBConstant.MSG_TYPE_SINGLE_TEXT) {
                if (isMsgGif(msg)) {
                    msg.setGIfEmo(true);
                } else {
                    msg.setGIfEmo(false);
                }
            }
            nextTime = msg.getCreated();
            boolean needTimeBubble = DateUtil.needDisplayTime(preTime, nextTime);
            if (needTimeBubble) {
                Integer in = nextTime;
                chatList.add(in);
            }
            preTime = nextTime;
            if (msg.getDisplayType() == DBConstant.SHOW_MIX_TEXT) {
                MixMessage mixMessage = (MixMessage) msg;
                chatList.addAll(mixMessage.getMsgList());
            } else {
                chatList.add(msg);
            }
        }
        // ???????????????????????????????????????
        msgObjectList.addAll(0, chatList);
        getImageList();
        logger.d("#messageAdapter#addItem");
        notifyDataSetChanged();
    }

    /**
     * ????????????????????????
     */
    private void getImageList() {
        for (int i = msgObjectList.size() - 1; i >= 0; --i) {
            Object item = msgObjectList.get(i);
            if (item instanceof ImageMessage) {
                ImageMessage.addToImageMessageList((ImageMessage) item);
            }
        }
    }

    /**
     * ??????????????????????????????
     */
    public void hidePopup() {
        if (currentPop != null) {
            currentPop.hidePopup();
        }
    }


    public void clearItem() {
        msgObjectList.clear();
    }

    /**
     * msgId ?????????ID
     * localId????????????ID
     * position ???list ?????????
     * <p/>
     * ?????????item?????????
     * ??????????????????
     * <p/>
     */
    public void updateItemState(int position, final MessageEntity messageEntity) {
        //??????DB
        //??????????????????
        imService.getDbInterface().insertOrUpdateMessage(messageEntity);
        notifyDataSetChanged();
    }

    /**
     * ?????????????????????????????????
     */
    public void updateItemState(final MessageEntity messageEntity) {
        long dbId = messageEntity.getId();
        int msgId = messageEntity.getMsgId();
        int len = msgObjectList.size();
        for (int index = len - 1; index > 0; index--) {
            Object object = msgObjectList.get(index);
            if (object instanceof MessageEntity) {
                MessageEntity entity = (MessageEntity) object;
                if (object instanceof ImageMessage) {
                    ImageMessage.addToImageMessageList((ImageMessage) object);
                }
                if (entity.getId() == dbId && entity.getMsgId() == msgId) {
                    msgObjectList.set(index, messageEntity);
                    break;
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (null == msgObjectList) {
            return 0;
        } else {
            return msgObjectList.size();
        }
    }

    @Override
    public int getViewTypeCount() {
        return RenderType.values().length;
    }


    @Override
    public int getItemViewType(int position) {
        try {
            /**?????????????????????*/
            RenderType type = RenderType.MESSAGE_TYPE_INVALID;

            Object obj = msgObjectList.get(position);
            if (obj instanceof Integer) {
                type = RenderType.MESSAGE_TYPE_TIME_TITLE;
            } else if (obj instanceof MessageEntity) {
                MessageEntity info = (MessageEntity) obj;
                boolean isMine = info.getFromId() == loginUser.getPeerId();
                switch (info.getDisplayType()) {
                    case DBConstant.SHOW_AUDIO_TYPE:
                        type = isMine ? RenderType.MESSAGE_TYPE_MINE_AUDIO
                                : RenderType.MESSAGE_TYPE_OTHER_AUDIO;
                        break;
                    case DBConstant.SHOW_IMAGE_TYPE:
                        ImageMessage imageMessage = (ImageMessage) info;
                        if (CommonUtil.gifCheck(imageMessage.getUrl())) {
                            type = isMine ? RenderType.MESSAGE_TYPE_MINE_GIF_IMAGE
                                    : RenderType.MESSAGE_TYPE_OTHER_GIF_IMAGE;
                        } else {
                            type = isMine ? RenderType.MESSAGE_TYPE_MINE_IMAGE
                                    : RenderType.MESSAGE_TYPE_OTHER_IMAGE;
                        }

                        break;
                    case DBConstant.SHOW_ORIGIN_TEXT_TYPE:
                        if (info.isGIfEmo()) {
                            type = isMine ? RenderType.MESSAGE_TYPE_MINE_GIF
                                    : RenderType.MESSAGE_TYPE_OTHER_GIF;
                        } else {
                            type = isMine ? RenderType.MESSAGE_TYPE_MINE_TETX
                                    : RenderType.MESSAGE_TYPE_OTHER_TEXT;
                        }

                        break;
                    case DBConstant.SHOW_MIX_TEXT:
                        //
                        logger.e("?????????????????????%s", obj);
                    default:
                        break;
                }
            }
            return type.ordinal();
        } catch (Exception e) {
            logger.e(e.getMessage());
            return RenderType.MESSAGE_TYPE_INVALID.ordinal();
        }
    }

    @Override
    public Object getItem(int position) {
        if (position >= getCount() || position < 0) {
            return null;
        }
        return msgObjectList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    /**
     * ???????????????????????????
     */
    private View timeBubbleRender(int position, View convertView, ViewGroup parent) {
        TimeRenderView timeRenderView;
        Integer timeBubble = (Integer) msgObjectList.get(position);
        if (null == convertView) {
            timeRenderView = TimeRenderView.inflater(ctx, parent);
        } else {
            // ???????????????tag ?????????
            timeRenderView = (TimeRenderView) convertView;
        }
        timeRenderView.setTime(timeBubble);
        return timeRenderView;
    }

    /**
     * 1.????????????
     * mine:?????? other??????
     * ???????????????  ????????????????????????????????????????????????????????????
     * ?????????????????????  ????????????
     * <p/>
     * ?????????????????????render
     *
     * @param position
     * @param convertView
     * @param parent
     * @param isMine
     * @return
     */
    private View imageMsgRender(final int position, View convertView, final ViewGroup parent, final boolean isMine) {
        ImageRenderView imageRenderView;
        final ImageMessage imageMessage = (ImageMessage) msgObjectList.get(position);
        UserEntity userEntity = imService.getContactManager().findContact(imageMessage.getFromId());

        /**??????????????????path*/
        final String imagePath = imageMessage.getPath();
        /**????????????image??????*/
        final String imageUrl = imageMessage.getUrl();

        if (null == convertView) {
            imageRenderView = ImageRenderView.inflater(ctx, parent, isMine);
        } else {
            imageRenderView = (ImageRenderView) convertView;
        }

        final ImageView messageImage = imageRenderView.getMessageImage();
        final int msgId = imageMessage.getMsgId();
        imageRenderView.setBtnImageListener(new ImageRenderView.BtnImageListener() {
            @Override
            public void onMsgFailure() {
                /**
                 * ????????????????????????????????????????????????
                 * ??????isMine????????????????????????????????????
                 * 1. ???????????????????????????????????????????????[??????????????????]
                 * 2. ?????????????????????????????????????????? ????????????????????
                 */
                if (FileUtil.isSdCardAvailuable()) {
//                    imageMessage.setLoadStatus(MessageStatus.IMAGE_UNLOAD);//???????????????????????????????????????
                    imageMessage.setStatus(MessageConstant.MSG_SENDING);
                    if (imService != null) {
                        imService.getMessageManager().resendMessage(imageMessage);
                    }
                    updateItemState(msgId, imageMessage);
                } else {
                    Toast.makeText(ctx, ctx.getString(R.string.sdcard_unavaluable), Toast.LENGTH_LONG).show();
                }
            }

            //DetailPortraitActivity ???????????????DisplayImageActivity ?????????
            @Override
            public void onMsgSuccess() {
                Intent i = new Intent(ctx, PreviewMessageImagesActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable(IntentConstant.CUR_MESSAGE, imageMessage);
                i.putExtras(bundle);
                ctx.startActivity(i);
                ((Activity) ctx).overridePendingTransition(R.anim.tt_image_enter, R.anim.tt_stay);
            }
        });

        // ????????????loadImage?????????
        imageRenderView.setImageLoadListener(new ImageRenderView.ImageLoadListener() {

            @Override
            public void onLoadComplete(String loaclPath) {
                logger.d("chat#pic#save image ok");
                logger.d("pic#setsavepath:%s", loaclPath);
//                imageMessage.setPath(loaclPath);//?????????????????????????????????
                imageMessage.setLoadStatus(MessageConstant.IMAGE_LOADED_SUCCESS);
                updateItemState(imageMessage);
            }

            @Override
            public void onLoadFailed() {
                logger.d("chat#pic#onBitmapFailed");
                imageMessage.setLoadStatus(MessageConstant.IMAGE_LOADED_FAILURE);
                updateItemState(imageMessage);
                logger.d("download failed");
            }
        });

        final View messageLayout = imageRenderView.getMessageLayout();
        messageImage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // ????????????pop??????????????? ????????????????????????????????????????????????
                MessageOperatePopup popup = getPopMenu(parent, new OperateItemClickListener(imageMessage, position));
                boolean bResend = (imageMessage.getStatus() == MessageConstant.MSG_FAILURE)
                        || (imageMessage.getLoadStatus() == MessageConstant.IMAGE_UNLOAD);
                popup.show(messageLayout, DBConstant.SHOW_IMAGE_TYPE, bResend, isMine);
                return true;
            }
        });

        /**??????????????????????????????view*/
        imageRenderView.getMessageFailed().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // ????????????????????????
                MessageOperatePopup popup = getPopMenu(parent, new OperateItemClickListener(imageMessage, position));
                popup.show(messageLayout, DBConstant.SHOW_IMAGE_TYPE, true, isMine);
            }
        });
        imageRenderView.render(imageMessage, userEntity, ctx);

        return imageRenderView;
    }

    private View GifImageMsgRender(final int position, View convertView, final ViewGroup parent, final boolean isMine) {
        GifImageRenderView imageRenderView;
        final ImageMessage imageMessage = (ImageMessage) msgObjectList.get(position);
        UserEntity userEntity = imService.getContactManager().findContact(imageMessage.getFromId());
        if (null == convertView) {
            imageRenderView = GifImageRenderView.inflater(ctx, parent, isMine);
        } else {
            imageRenderView = (GifImageRenderView) convertView;
        }
        GifView imageView = imageRenderView.getMessageContent();
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                final String url = imageMessage.getUrl();
                Intent intent = new Intent(ctx, PreviewGifActivity.class);
                intent.putExtra(IntentConstant.PREVIEW_TEXT_CONTENT, url);
                ctx.startActivity(intent);
                ((Activity) ctx).overridePendingTransition(R.anim.tt_image_enter, R.anim.tt_stay);
            }
        });
        imageRenderView.render(imageMessage, userEntity, ctx);
        return imageRenderView;
    }

    /**
     * ???????????????????????????????????????
     * ???????????????
     * ??????????????????
     * ???????????????????????????/
     * ??????????????????
     *
     * @param position
     * @param convertView
     * @param parent
     * @param isMine
     * @return
     */
    private View audioMsgRender(final int position, View convertView, final ViewGroup parent, final boolean isMine) {
        AudioRenderView audioRenderView;
        final AudioMessage audioMessage = (AudioMessage) msgObjectList.get(position);
        UserEntity entity = imService.getContactManager().findContact(audioMessage.getFromId());
        if (null == convertView) {
            audioRenderView = AudioRenderView.inflater(ctx, parent, isMine);
        } else {
            audioRenderView = (AudioRenderView) convertView;
        }
        final String audioPath = audioMessage.getAudioPath();

        final View messageLayout = audioRenderView.getMessageLayout();
        if (!TextUtils.isEmpty(audioPath)) {
            // ?????????????????????,??????????????????????????????
            messageLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    MessageOperatePopup popup = getPopMenu(parent, new OperateItemClickListener(audioMessage, position));
                    boolean bResend = audioMessage.getStatus() == MessageConstant.MSG_FAILURE;
                    popup.show(messageLayout, DBConstant.SHOW_AUDIO_TYPE, bResend, isMine);
                    return true;
                }
            });
        }


        audioRenderView.getMessageFailed().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                MessageOperatePopup popup = getPopMenu(parent, new OperateItemClickListener(audioMessage, position));
                popup.show(messageLayout, DBConstant.SHOW_AUDIO_TYPE, true, isMine);
            }
        });


        audioRenderView.setBtnImageListener(new AudioRenderView.BtnImageListener() {
            @Override
            public void onClickUnread() {
                logger.d("chat#audio#set audio meessage read status");
                audioMessage.setReadStatus(MessageConstant.AUDIO_READED);
                imService.getDbInterface().insertOrUpdateMessage(audioMessage);
            }

            @Override
            public void onClickReaded() {
            }
        });
        audioRenderView.render(audioMessage, entity, ctx);
        return audioRenderView;
    }


    /**
     * text?????????: 1. ????????????Emoparser
     * 2. ????????????  ??????????????? ?????????????????????pop menu
     * ????????????????????? ??????
     *
     * @param position
     * @param convertView
     * @param viewGroup
     * @param isMine
     * @return
     */
    private View textMsgRender(final int position, View convertView, final ViewGroup viewGroup, final boolean isMine) {
        TextRenderView textRenderView;
        final TextMessage textMessage = (TextMessage) msgObjectList.get(position);
        UserEntity userEntity = imService.getContactManager().findContact(textMessage.getFromId());

        if (null == convertView) {
            textRenderView = TextRenderView.inflater(ctx, viewGroup, isMine); //new TextRenderView(ctx,viewGroup,isMine);
        } else {
            textRenderView = (TextRenderView) convertView;
        }

        final TextView textView = textRenderView.getMessageContent();

        // ??????????????????
        textRenderView.getMessageFailed().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                MessageOperatePopup popup = getPopMenu(viewGroup, new OperateItemClickListener(textMessage, position));
                popup.show(textView, DBConstant.SHOW_ORIGIN_TEXT_TYPE, true, isMine);
            }
        });

        textView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // ????????????
                MessageOperatePopup popup = getPopMenu(viewGroup, new OperateItemClickListener(textMessage, position));
                boolean bResend = textMessage.getStatus() == MessageConstant.MSG_FAILURE;
                popup.show(textView, DBConstant.SHOW_ORIGIN_TEXT_TYPE, bResend, isMine);
                return true;
            }
        });

        // url ?????????????????? ????????????
        final String content = textMessage.getContent();
        textView.setOnTouchListener(new OnDoubleClickListener() {
            @Override
            public void onClick(View view) {
                //todo
            }

            @Override
            public void onDoubleClick(View view) {
                Intent intent = new Intent(ctx, PreviewTextActivity.class);
                intent.putExtra(IntentConstant.PREVIEW_TEXT_CONTENT, content);
                ctx.startActivity(intent);
            }
        });
        textRenderView.render(textMessage, userEntity, ctx);
        return textRenderView;
    }

    /**
     * ???????????????gif???????????????: 1. ????????????Emoparser
     * 2. ????????????  ??????????????? ?????????????????????pop menu
     * ????????????????????? ??????
     *
     * @param position
     * @param convertView
     * @param viewGroup
     * @param isMine
     * @return
     */
    private View gifMsgRender(final int position, View convertView, final ViewGroup viewGroup, final boolean isMine) {
        EmojiRenderView gifRenderView;
        final TextMessage textMessage = (TextMessage) msgObjectList.get(position);
        UserEntity userEntity = imService.getContactManager().findContact(textMessage.getFromId());
        if (null == convertView) {
            gifRenderView = EmojiRenderView.inflater(ctx, viewGroup, isMine); //new TextRenderView(ctx,viewGroup,isMine);
        } else {
            gifRenderView = (EmojiRenderView) convertView;
        }

        final ImageView imageView = gifRenderView.getMessageContent();
        // ??????????????????
        gifRenderView.getMessageFailed().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                MessageOperatePopup popup = getPopMenu(viewGroup, new OperateItemClickListener(textMessage, position));
                popup.show(imageView, DBConstant.SHOW_GIF_TYPE, true, isMine);
            }
        });

        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                MessageOperatePopup popup = getPopMenu(viewGroup, new OperateItemClickListener(textMessage, position));
                boolean bResend = textMessage.getStatus() == MessageConstant.MSG_FAILURE;
                popup.show(imageView, DBConstant.SHOW_GIF_TYPE, bResend, isMine);

                return true;
            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                final String content = textMessage.getContent();
                Intent intent = new Intent(ctx, PreviewGifActivity.class);
                intent.putExtra(IntentConstant.PREVIEW_TEXT_CONTENT, content);
                ctx.startActivity(intent);
                ((Activity) ctx).overridePendingTransition(R.anim.tt_image_enter, R.anim.tt_stay);
            }
        });

        gifRenderView.render(textMessage, userEntity, ctx);
        return gifRenderView;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        try {
            final int typeIndex = getItemViewType(position);
            RenderType renderType = RenderType.values()[typeIndex];
            // ??????map?????????
            switch (renderType) {
                case MESSAGE_TYPE_INVALID:
                    // ????????????
                    logger.e("[fatal erro] render type:MESSAGE_TYPE_INVALID");
                    break;

                case MESSAGE_TYPE_TIME_TITLE:
                    convertView = timeBubbleRender(position, convertView, parent);
                    break;

                case MESSAGE_TYPE_MINE_AUDIO:
                    convertView = audioMsgRender(position, convertView, parent, true);
                    break;
                case MESSAGE_TYPE_OTHER_AUDIO:
                    convertView = audioMsgRender(position, convertView, parent, false);
                    break;
                case MESSAGE_TYPE_MINE_GIF_IMAGE:
                    convertView = GifImageMsgRender(position, convertView, parent, true);
                    break;
                case MESSAGE_TYPE_OTHER_GIF_IMAGE:
                    convertView = GifImageMsgRender(position, convertView, parent, false);
                    break;
                case MESSAGE_TYPE_MINE_IMAGE:
                    convertView = imageMsgRender(position, convertView, parent, true);
                    break;
                case MESSAGE_TYPE_OTHER_IMAGE:
                    convertView = imageMsgRender(position, convertView, parent, false);
                    break;
                case MESSAGE_TYPE_MINE_TETX:
                    convertView = textMsgRender(position, convertView, parent, true);
                    break;
                case MESSAGE_TYPE_OTHER_TEXT:
                    convertView = textMsgRender(position, convertView, parent, false);
                    break;

                case MESSAGE_TYPE_MINE_GIF:
                    convertView = gifMsgRender(position, convertView, parent, true);
                    break;
                case MESSAGE_TYPE_OTHER_GIF:
                    convertView = gifMsgRender(position, convertView, parent, false);
                    break;
            }
            return convertView;
        } catch (Exception e) {
            logger.e("chat#%s", e);
            return null;
        }
    }

    /**
     * ?????????????????????
     */
    private MessageOperatePopup getPopMenu(ViewGroup parent, MessageOperatePopup.OnItemClickListener listener) {
        MessageOperatePopup popupView = MessageOperatePopup.instance(ctx, parent);
        currentPop = popupView;
        popupView.setOnItemClickListener(listener);
        return popupView;
    }

    private class OperateItemClickListener
            implements
            MessageOperatePopup.OnItemClickListener {

        private MessageEntity mMsgInfo;
        private int mType;
        private int mPosition;

        public OperateItemClickListener(MessageEntity msgInfo, int position) {
            mMsgInfo = msgInfo;
            mType = msgInfo.getDisplayType();
            mPosition = position;
        }

        @SuppressWarnings("deprecation")
        @SuppressLint("NewApi")
        @Override
        public void onCopyClick() {
            try {
                ClipboardManager manager = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);

                logger.d("menu#onCopyClick content:%s", mMsgInfo.getContent());
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                    ClipData data = ClipData.newPlainText("data", mMsgInfo.getContent());
                    manager.setPrimaryClip(data);
                } else {
                    manager.setText(mMsgInfo.getContent());
                }
            } catch (Exception e) {
                logger.e(e.getMessage());
            }
        }

        @Override
        public void onResendClick() {
            try {
                if (mType == DBConstant.SHOW_AUDIO_TYPE
                        || mType == DBConstant.SHOW_ORIGIN_TEXT_TYPE) {

                    if (mMsgInfo.getDisplayType() == DBConstant.SHOW_AUDIO_TYPE) {
                        if (mMsgInfo.getSendContent().length < 4) {
                            return;
                        }
                    }
                } else if (mType == DBConstant.SHOW_IMAGE_TYPE) {
                    logger.d("pic#resend");
                    // ???????????????????????? ??????????????????????????????
                    // ???????????????????????????
                    ImageMessage imageMessage = (ImageMessage) mMsgInfo;
                    if (TextUtils.isEmpty(imageMessage.getPath())) {
                        Toast.makeText(ctx, ctx.getString(R.string.image_path_unavaluable), Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                mMsgInfo.setStatus(MessageConstant.MSG_SENDING);
                msgObjectList.remove(mPosition);
                addItem(mMsgInfo);
                if (imService != null) {
                    imService.getMessageManager().resendMessage(mMsgInfo);
                }

            } catch (Exception e) {
                logger.e("chat#exception:" + e.toString());
            }
        }

        @Override
        public void onSpeakerClick() {
            AudioPlayerHandler audioPlayerHandler = AudioPlayerHandler.getInstance();
            if (audioPlayerHandler.getAudioMode(ctx) == AudioManager.MODE_NORMAL) {
                audioPlayerHandler.setAudioMode(AudioManager.MODE_IN_CALL, ctx);
                SpeekerToast.show(ctx, ctx.getText(R.string.audio_in_call), Toast.LENGTH_SHORT);
            } else {
                audioPlayerHandler.setAudioMode(AudioManager.MODE_NORMAL, ctx);
                SpeekerToast.show(ctx, ctx.getText(R.string.audio_in_speeker), Toast.LENGTH_SHORT);
            }
        }
    }
}
