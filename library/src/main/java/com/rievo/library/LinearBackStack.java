package com.rievo.library;

import android.content.Context;
import android.support.v4.view.AsyncLayoutInflater;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Created by kwang on 2017-09-14.
 */

/**
 * A linear backstack flow
 */
public class LinearBackStack extends RelativeLayout implements BStack {
    protected String TAG;

    protected LayoutInflater layoutInflater;
    protected LinearState s;

    protected List<Pair<Node, ViewGroup>> viewStack = new ArrayList<>();

    //***************************************************
    // Lifecycle
    //*******************************************

    //<editor-fold desc="Lifecycle">

    //******************************
    // Initialization
    //***************************

    public LinearBackStack(Context context, String TAG, Node firstNode) {
        super(context);

        this.TAG = TAG;
        layoutInflater = LayoutInflater.from(context);
        BackStack.getBackStackManager().addStack(TAG, this);

        s = getState();
        if (s == null){
            BackStack.getBackStackManager().addLinearState(TAG);

            s = getState();
            s.nodes.add(firstNode);
            addView(firstNode);
            //activity start up, first time
        } else {
            //Readd all of the retained views
            for (int counter = 0; counter < s.nodes.size(); counter++) {
                Node node = s.nodes.get(counter);

                if (node.shouldRetain() && counter < s.nodes.size() - 1) {
                    addView(node);
                }
            }

            //Readd the top view of the stack
            addView(Helper.getLast(s.nodes));
        }
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    public void destroy() {

    }
    //</editor-fold>

    //*****************************
    // Functional
    //*****************************

    public String getTag(){
        return TAG;
    }

    public boolean add(ViewCreator viewCreator){
        return add(Node.builder().viewCreator(viewCreator).build());
    }

    public boolean add(Node node){
        Timber.d("add");

        s.nodes.add(node);

        final Pair<Node, ViewGroup> prev = Helper.getLast(viewStack);
        Helper.disable(prev.second);

        addView(node);
        onAddEvent();

        if (node.addAnimator() != null) {
            node.addAnimator().animate(Helper.getLast(viewStack).second, new Emitter() {
                @Override
                public void done() {
                    if (!prev.first.shouldRetain()) {
                        removeView(prev.second);
                    }
                }
            });
        } else {
            if (!prev.first.shouldRetain()) {
                removeView(prev.second);
            }
        }

        return true;
    }

    public boolean addAsync(Node node){
        if (node.asyncViewCreator() == null){
            throw new RuntimeException("AsyncViewCreator is not set");
        }

        return true;
    }

    @Override
    public boolean goBack() {
        Timber.d("go back");

        if (s.nodes.size() <= 1){
            Timber.d("last node");
            return false;
        }

        //Get the previous node
        Node prev = s.nodes.get(s.nodes.size() - 2);
        ViewGroup prevView = null;
        if (!prev.shouldRetain()){
            prevView = prev.viewCreator().create(layoutInflater, this);
            viewStack.add(viewStack.size() - 1, new Pair<>(prev, prevView));
            addView(prevView);
        }

        //pops view from stack
        final ViewGroup currentView = Helper.pop(viewStack).second;
        final Node currentNode = Helper.pop(s.nodes);

        //Remove the view. If the animator isn't null then use that first
        if (currentNode.removeAnimator() != null) {
            currentView.bringToFront();
            currentNode.removeAnimator().animate(currentView, new Emitter() {
                @Override
                public void done() {
                    Timber.d("hi");

                    //pops view from stack and removes view from parent
                    removeView(currentView);
                }
            });
        } else {
            removeView(currentView);
        }

        return true;
    }

    public boolean goToHome(){
        if (s.nodes.size() <= 1){
            Timber.d("last node");
            return false;
        }

        Node first = s.nodes.get(0);
        ViewGroup firstView = null;
        if (!first.shouldRetain()){
            firstView = first.viewCreator().create(layoutInflater, this);
            viewStack.add(0, new Pair<>(first, firstView));
        }

        for (int i = viewStack.size() - 1; i > 0; i-- ){
            removeView(Helper.pop(viewStack).second);
        }

        for (int i = s.nodes.size() - 1; i > 0; i--){
            Helper.pop(s.nodes);
        }

        return true;
    }

    //*****************************
    // Helper
    //********************

    public LinearState getState(){
        return BackStack.getBackStackManager().getLinearState(TAG);
    }

    public ViewGroup addView(Node node){
        ViewGroup vg = node.viewCreator().create(layoutInflater, this);
        viewStack.add(new Pair<>(node, vg));
        addView(vg);

        return vg;
    }

    //**************

    protected List<Listener> listenerList = new ArrayList<>();

    public void addListener(Listener listener){
        listenerList.add(listener);
    }

    public void onAddEvent(){
        for (Listener listener: listenerList){
            listener.onAdd(TAG);
        }
    }

    interface Listener{
        void onAdd(String TAG);
    }
}
