package com.home.lepradroid.tasks;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.text.TextUtils;
import android.util.Pair;

import com.home.lepradroid.commons.Commons;
import com.home.lepradroid.interfaces.PostsUpdateListener;
import com.home.lepradroid.interfaces.UpdateListener;
import com.home.lepradroid.listenersworker.ListenersWorker;
import com.home.lepradroid.objects.Post;
import com.home.lepradroid.serverworker.ServerWorker;
import com.home.lepradroid.utils.Logger;

public class GetPostsTask extends BaseTask
{
    static final Class<?>[] argsClasses = new Class[0];
    static Method method;
    static 
    {
        try
        {
            method = PostsUpdateListener.class.getMethod("OnPostsUpdate", argsClasses);    
        }
        catch (Throwable t) 
        {           
            Logger.e(t);
        }        
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Throwable doInBackground(Void... params)
    {
        try
        {
            final String html = ServerWorker.Instance().getContent(Commons.SITE_URL);
            final Document document = Jsoup.parse(html);
            final Element content = document.getElementById("content");
            final Elements posts = content.getElementsByClass("dt");
            for (@SuppressWarnings("rawtypes")
            Iterator iterator = posts.iterator(); iterator.hasNext();)
            {
                Element element = (Element) iterator.next();
                String text = element.text();
                Post post = new Post();
                post.Text = TextUtils.isEmpty(text) ? "..." : text;
                post.Html = element.html();
                ServerWorker.Instance().addNewPost(post);
            }
        }
        catch (Throwable t)
        {
            setException(t);
        }
        finally
        {
            final List<PostsUpdateListener> listeners = ListenersWorker.Instance().getListeners(PostsUpdateListener.class);
            final Object args[] = new Object[0];
            
            for(PostsUpdateListener listener : listeners)
            {
                publishProgress(new Pair<UpdateListener, Pair<Method, Object[]>>(listener, new Pair<Method, Object[]> (method, args)));
            }
        }
        
        return e;
    }
}