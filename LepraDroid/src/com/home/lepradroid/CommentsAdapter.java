package com.home.lepradroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.home.lepradroid.commons.Commons;
import com.home.lepradroid.commons.Commons.RateValueType;
import com.home.lepradroid.interfaces.ExitListener;
import com.home.lepradroid.objects.BaseItem;
import com.home.lepradroid.objects.Comment;
import com.home.lepradroid.settings.SettingsWorker;
import com.home.lepradroid.tasks.RateItemTask;
import com.home.lepradroid.tasks.TaskWrapper;
import com.home.lepradroid.utils.LinksCatcher;
import com.home.lepradroid.utils.Utils;

class CommentsAdapter extends ArrayAdapter<BaseItem> implements ExitListener
{   
    private UUID postId;
    private UUID groupId;
    private List<BaseItem>      comments            = Collections.synchronizedList(new ArrayList<BaseItem>());
    private GestureDetector     gestureDetector;
    private int                 commentPos          = -1;
    private int                 commentLevelIndicatorLength
                                                    = 0;
    private LayoutInflater      aInflater           = null;
    
    private void OnLongClick()
    {
        final Comment item = (Comment)getItem(commentPos);
        List<String> actions = new ArrayList<String>(0);
        actions.add(Utils.getString(R.string.CommentAction_Author));
        actions.add(Utils.getString(R.string.CommentAction_Reply));
        if(     !item.getAuthor().equalsIgnoreCase(SettingsWorker.Instance().loadUserName()) &&
                !groupId.equals(Commons.INBOX_POSTS_ID))
        {
            if(!item.isPlusVoted())
                actions.add(Utils.getString(R.string.CommentAction_Like));
            if(!item.isMinusVoted())
                actions.add(Utils.getString(R.string.CommentAction_Dislike));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Выберите действие");
        builder.setItems(actions.toArray(new String[actions.size()]), new DialogInterface.OnClickListener() 
        {
            public void onClick(DialogInterface dialog, int pos) 
            {
                switch (pos)
                {
                case 0:
                    Intent intent = new Intent(getContext(), AuthorScreen.class);
                    intent.putExtra("username", item.getAuthor());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    LepraDroidApplication.getInstance().startActivity(intent); 
                    break;
                case 1:
                    Utils.addComment(getContext(), groupId, postId, item.getId());
                    break;
                case 2:
                    if(!item.isPlusVoted())
                        new TaskWrapper(null, new RateItemTask(groupId, postId, item.getId(), RateValueType.PLUS), "");
                    else 
                        new TaskWrapper(null, new RateItemTask(groupId, postId, item.getId(), RateValueType.MINUS), "");
                    break;
                case 3:
                    new TaskWrapper(null, new RateItemTask(groupId, postId, item.getId(), RateValueType.MINUS), "");
                    break;
                default:
                    break;
                }
            }
        });
        builder.create().show();
    }
      
    public CommentsAdapter(Context context, final UUID groupId, final UUID postId, int textViewResourceId,
            ArrayList<BaseItem> comments)
    {
        super(context, textViewResourceId, comments);
        this.comments = comments;
        this.postId = postId;
        this.groupId = groupId;
        
        aInflater = LayoutInflater.from(getContext());
        
        commentLevelIndicatorLength = Utils.getCommentLevelIndicatorLength();
        
        gestureDetector = new GestureDetector(
                new GestureDetector.SimpleOnGestureListener()
                {
                    @Override
                    public void onLongPress(MotionEvent e)
                    {
                        OnLongClick();
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e)
                    {
                        return false;
                    }

                    @Override
                    public boolean onDown(MotionEvent e)
                    {
                        return false;
                    }
                });
        gestureDetector.setIsLongpressEnabled(true);
    }

    public int getCount() 
    {
        return comments.size();
    }
    
    public BaseItem getItem(int position) 
    {
        return comments.get(position);
    }
    
    public long getItemId(int position) 
    { 
        return position;
    }
    
    public void updateData(ArrayList<BaseItem> comments)
    {
        boolean containProgressElement = isContainProgressElement();
        
        this.comments.clear();
        this.comments.addAll(comments);
        
        if(containProgressElement)
            addProgressElement();
    }
    
    public void addProgressElement()
    {
        if(!comments.isEmpty() && comments.get(comments.size() - 1) != null)
            comments.add(null);   
    }
    
    public boolean isContainProgressElement()
    {
        return !comments.isEmpty() && comments.get(comments.size() - 1) == null;
    }
    
    public void removeProgressElement()
    {
        if(!comments.isEmpty() && comments.get(comments.size() - 1) == null)
            comments.remove(null);
    }
    
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) 
    {
        Comment comment = (Comment)getItem(position);
        
        if(comment != null)
        {
            convertView = aInflater.inflate(R.layout.comments_row_view, parent, false);
            
            short level = comment.getLevel();
            
            FrameLayout root = (FrameLayout)convertView.findViewById(R.id.root);
            CommentRootLayout content = (CommentRootLayout)convertView.findViewById(R.id.content);
            
            content.setLevel(level);
            
            if(level > 0)
            {
                root.setPadding(root.getPaddingLeft() + (level * commentLevelIndicatorLength), root.getPaddingTop(), root.getPaddingRight(), root.getPaddingBottom());
                content.setPadding(content.getPaddingLeft() * 2, content.getPaddingTop(), content.getPaddingRight(), content.getPaddingBottom());
            }
            
            if(!comment.isOnlyText())
            {
                FrameLayout webContainer = (FrameLayout) convertView.findViewById(R.id.web_container);
                webContainer.setVisibility(View.VISIBLE);
                WebView webView = new WebView(getContext());
                webContainer.addView(webView);
                //webView.setVerticalFadingEdgeEnabled(true);
                //webView.setFadingEdgeLength(Utils.getStandardPedding());
                webView.setBackgroundColor(0x00000000);
                webView.setVisibility(View.VISIBLE);
                webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
                webView.setWebViewClient(LinksCatcher.Instance());
                WebSettings webSettings = webView.getSettings();
                webSettings.setDefaultFontSize(Commons.WEBVIEW_DEFAULT_FONT_SIZE);
                webSettings.setJavaScriptEnabled(true);
                webView.loadDataWithBaseURL("", Commons.WEBVIEW_HEADER + "<body style=\"margin: 0; padding: 0\">" + comment.getHtml() + "</body>", "text/html", "UTF-8", null);
                webView.addJavascriptInterface(ImagesWorker.Instance(), "ImagesWorker");
                webView.setOnTouchListener(new View.OnTouchListener() 
                {
                    @Override
                    public boolean onTouch(View arg0, MotionEvent event)
                    {
                        commentPos = position;
                        return gestureDetector.onTouchEvent(event);
                    }
                });
                
                Utils.setWebViewFontSize(webView);
            }
            else
            {
                TextView textOnly = (TextView)convertView.findViewById(R.id.textOnly);
                textOnly.setVisibility(View.VISIBLE);
                textOnly.setMovementMethod(LinkMovementMethod.getInstance());
                textOnly.setText(Html.fromHtml(comment.getHtml()));
                textOnly.setOnLongClickListener(new View.OnLongClickListener()
                {
                    @Override
                    public boolean onLongClick(View v)
                    {
                        commentPos = position;
                        OnLongClick();
                        return false;
                    }
                });
                
                Utils.setTextViewFontSize(textOnly);
            }
            
            TextView author = (TextView)convertView.findViewById(R.id.author);
            author.setText(Html.fromHtml(comment.getSignature()));
            Utils.setTextViewFontSize(author);
            
            TextView rating = (TextView)convertView.findViewById(R.id.rating);
            if(!groupId.equals(Commons.INBOX_POSTS_ID))
            {
                rating.setText(Utils.getRatingStringFromBaseItem(comment));
                Utils.setTextViewFontSize(rating);
            }
            else
                rating.setVisibility(View.GONE);   
            
            if(comment.isNew())
            {
                root.setBackgroundColor(Color.parseColor("#FFE6E6E6"));
                content.setBackgroundColor(Color.parseColor("#FFE6E6E6"));
            }
        }
        else
        {
            convertView = aInflater.inflate(R.layout.footer_view, null);
        }

        return convertView;
    }

    @Override
    public void OnExit()
    {
        // TODO Auto-generated method stub
    }
}
