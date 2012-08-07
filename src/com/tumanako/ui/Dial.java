package com.tumanako.ui;

/************************************************************************************
Tumanako - Electric Vehicle and Motor control software

Copyright (C) 2012 Jeremy Cole-Baker <jeremy@rhtech.co.nz>

This file is part of Tumanako Dashboard.

Tumanako is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Tumanako is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with Tumanako.  If not, see <http://www.gnu.org/licenses/>.

*************************************************************************************/

import com.tumanako.dash.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.View;


/*************************************************************************************
 * 
 * Dial: Experimental!
 *
 * Custom Attributes: 
 * 
 *   minimum_scale    - float
 *   scale_step       - float
 *   number_divisions - integer
 *   scale_tick_step  - float
 *   minimum_angle    - integer
 *   maximum_angle    - integer
 *   origin_x         - float
 *   origin_y         - float
 *   needle_length    - float
 *   dial_label       - string
 *   label_x          - float
 *   label_y          - float
 *   red_line         - float
 *   label_format     - string
 *   
 * Note that there must also be a values\attrs.xml file which defines the custom 
 * attributes.  It should look like this: 
 *
    <?xml version="1.0" encoding="utf-8"?>
     <resources>
     <declare-styleable name="Dial">
      <attr name="minimum_scale"     format="float" /> 
      <attr name="scale_step"        format="float" />
      <attr name="number_divisions"  format="integer" />
      <attr name="scale_tick_step"   format="float" />
      <attr name="minimum_angle"     format="integer" />
      <attr name="maximum_angle"     format="integer" />
      <attr name="origin_x"          format="float" />
      <attr name="origin_y"          format="float" />
      <attr name="needle_length"     format="float" />
      <attr name="dial_label"        format="string" />
      <attr name="label_x"           format="float" />
      <attr name="label_y"           format="float" />            
      <attr name="red_line"          format="float" />
      <attr name="label_format"      format="string" />
     </declare-styleable>
     </resources>

 * 
 * @author Jeremy Cole-Baker / Riverhead Technology
 *
 ************************************************************************************/



public class Dial extends View
  {

  private int drawingWidth = 0;
  private int drawingHeight = 0;

  // Internal constants to remember the dial attributes:  
  private float scaleMin = 0f; 
  private float scaleMax = 0f;
  private float deltaScale = 0f;
  private float scaleStep = 0f; 
  private float minAngle = 0f; 
  private float maxAngle = 0f;
  private float deltaAngle = 0f;
  private float fOriginX = 0f;
  private float fOriginY = 0f;
  private float fNeedleLength = 0f;
  private int numberDivisions = 0;
  private float tickStep = 0f;
  private String dialLabel;
  private float fLabelX = 0f;
  private float fLabelY = 0f;
  private float redLine = 0f;
  private String labelFormat;

  
  // Calculated internal values (based on actual size of dial, and generated at runtime): 
  private float originX = 0f;       // Needle Origin in screen coordinates inside the canvas of the control 
  private float originY = 0f;       //
  private float needleLength = 0f;  // Length of needle in screen coordinates
  private float labelX = 0f;        // Dial label Origin in screen coordinates inside the canvas of the control 
  private float labelY = 0f;        //
  private float[] slabelX;          // } Will contain the X and Y coordinates of the scale labels
  private float[] slabelY;          // }   once they have been calculated by setupDial method.
  private String[] scaleLabels;     //   Will contain the labels for the scale once they have been generated by setupDial. 
  
  // Runtime Data Values: 
  private float needleValue = 0f;   // The value we are currently representing
  private float needleAngle = 0f;   // The angle of the needle used to represent the above value 
  
  private Path needlePath;
  private PathShape needleShape;
  private ShapeDrawable needleDrawable;

  private Paint needlePaint;
  private Paint scalePaint = new Paint();
  
    
  // ********** Constructor: ***************************
  public Dial(Context context, AttributeSet atttibutes)
    {
    super(context, atttibutes);
    
    // Load custom attributes from XML: 
    getCustomAttributes(atttibutes);
    
    // Create the arrays to store pre-calculated scale data:     
    slabelX = new float[numberDivisions];
    slabelY = new float[numberDivisions];
    scaleLabels = new String[numberDivisions];

    // Create re-usable drawing objects used to draw the dial: 
    needlePath = new Path();  
    needleDrawable = new ShapeDrawable();
  
    // --DEBUG!-- Log.i( UIActivity.APP_TAG, "  Dial -> Constructor ");
    
    }
  
  
  
  
  
  
  /*********** Extract custom attributes: **************************
   * Given a set of attributes from the XML layout file, extract
   * the custom attributes specific to this control: 
   * @param attrs - Attributes passed in from the XML parser 
   *****************************************************************/
  private void getCustomAttributes(AttributeSet attrs)
    { 
    TypedArray a = getContext().obtainStyledAttributes( attrs, R.styleable.Dial );

    scaleMin        = a.getFloat(R.styleable.Dial_minimum_scale, 0f); 
    scaleStep       = a.getFloat(R.styleable.Dial_scale_step , 1f); 
    numberDivisions = a.getInt(R.styleable.Dial_number_divisions,5);
    tickStep        = a.getFloat(R.styleable.Dial_scale_tick_step, 0.5f);
    
    minAngle   = ((float)a.getInt(R.styleable.Dial_minimum_angle , -90) / 180f) * (float)Math.PI;  // NOTE: Angle attribute in Degrees; But store Radians. 
    maxAngle   = ((float)a.getInt(R.styleable.Dial_maximum_angle ,  90) / 180f) * (float)Math.PI;  // 
    deltaAngle = maxAngle - minAngle; 

    fOriginX      = a.getFloat(R.styleable.Dial_origin_x , 0.5f);
    fOriginY      = a.getFloat(R.styleable.Dial_origin_y , 0.5f);
    fNeedleLength = a.getFloat(R.styleable.Dial_needle_length , 0.35f);
    
    dialLabel = a.getString(R.styleable.Dial_dial_label);
    if (dialLabel == null) dialLabel = "";

    fLabelX = a.getFloat(R.styleable.Dial_label_x , 0.5f);
    fLabelY = a.getFloat(R.styleable.Dial_label_y , 0.3f);
    
    redLine = a.getFloat(R.styleable.Dial_red_line , 1f);
    
    labelFormat = a.getString(R.styleable.Dial_label_format);
    if (labelFormat == null) labelFormat = "%.0f";
    
    // Recycle the TypedArray: 
    a.recycle();

    }
  

  
  /******* Calculate run-time parameters for drawing scale, etc: ***************/
  private void calcDial()
    {
    // Calculate the needle origin in screen coordinates: 
    originX = (fOriginX * (float)drawingWidth);
    originY = (fOriginY * (float)drawingHeight);
    labelX = (fLabelX * (float)drawingWidth); 
    labelY = (fLabelY * (float)drawingHeight);

    // Calculate the needle length in screen coordinates: (Initially specified as a % of guage width):
    needleLength = (float)drawingWidth * fNeedleLength;   
    
    // Loop through the requested number of scale steps and calculate stuff...
    float scaleValue = scaleMin;     // Value of initial point on scale.
    float scaleAngle = minAngle;   // Angle from origin to initial point on scale in Radians (0 = vertical up)
    float scaleAngleStep = (maxAngle - minAngle) / (float)(numberDivisions-1);   // Angle step for each scale step
    for (int n=0; n<numberDivisions; n++)
      {
      scaleLabels[n] = String.format(labelFormat,scaleValue);
      slabelX[n] = needleX(scaleAngle);
      slabelY[n] = needleY(scaleAngle);
      
      scaleValue = scaleValue + scaleStep;
      scaleAngle = scaleAngle + scaleAngleStep;
      }
    scaleMax = scaleValue - scaleStep;              // Max scale value, used later.
    deltaScale = (float)scaleMax - (float)scaleMin; // Scale range.
    if (deltaScale == 0f) deltaScale = 1f;          // Oops. ??
    needleLength = needleLength * 0.9f;
    }

  
  
  
  /***** Set the value displayed on the dial: *******************
   *  
   * @param value Value to set the needle to
   */
  public void setNeedle(float value)
    {
    needleValue = value;
    needleAngle = minAngle + (((value - scaleMin) / deltaScale) * deltaAngle);
    invalidate();
    }
  
  
  /***** Get the current needle value: ******
   * @return Needle value
   */
  public float getNeedle()
    {  return needleValue;  }
  
  
  /******* Calculate screen X coordinate: *********
   * Given a needle angle (radians), calculate the corresponding screen
   * x coordinate for a point at the end of the needle. 
   * Uses originX, originY and needleLength which should already be set
   * (see setupDial method).  
   * 
   * @param thisAngle Needle angle in radians. 0 = Vertical Up. 
   * @return x screen coordinate
   * 
   */
  private float needleX(float thisAngle)
    {
    return originX + (needleLength * FloatMath.sin(thisAngle)); 
    }

  
  
  
  /******* Calculate screen Y coordinate: *********
   * Given a needle angle (radians), calculate the corresponding screen
   * y coordinate for a point at the end of the needle. 
   * Uses originX, originY and needleLength which should already be set
   * (see setupDial method).  
   * 
   * @param thisAngle Needle angle in radians. 0 = Vertical Up. 
   * @return y screen coordinate
   * 
   */
  private float needleY(float thisAngle)
    {
    return originY - (needleLength * FloatMath.cos(thisAngle)); 
    }
  
  
  
  
  
  
  private void makeNeedle()
    {
    // Make a line to represent the needle: 
    float x = needleX(needleAngle); 
    float y = needleY(needleAngle);
    needlePath.reset();
    needlePath.moveTo(originX, originY);             // ...Start point!
    needlePath.lineTo(x,y);                          // ...End point!
    }


  
  

  
  
  // **** Check a supplied width or hight spec, and decide whether to override it: 
  private int measureThis(int measureSpec) 
    {
    int result = 0;
    int specMode = MeasureSpec.getMode(measureSpec);
    int specSize = MeasureSpec.getSize(measureSpec);
    if (specMode == MeasureSpec.EXACTLY) 
      {
      // We were told how big to be, so we'll go with that!
      result = specSize;
      }
    else 
      {
      // We might have been given an indication of maximum size: 
      result = 320;   // We'll try 320 pixels. 
      if (specMode == MeasureSpec.AT_MOST) result = specSize;
         // Respect AT_MOST value if that was what is called for by measureSpec
      }
    return result;
    }


  
  
  
  // ********************** onMeasure: ************************************
  // We're being told how big we should be. Compute our dimensions: 
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) 
    {
    // The widthMeasureSpec and heightMeasureSpec contain width and height specifications packed into an integer. 
    // We need to carefully check and decode these. We'll use a couple of helper functions to do this: 
    setMeasuredDimension(measureThis(widthMeasureSpec),  measureThis(heightMeasureSpec));
    }

  
  
  
  @Override 
  protected void onDraw(Canvas canvas)
    {
    super.onDraw(canvas);
   
    // --DEBUG!--
    //Log.i( UIActivity.APP_TAG, "  Dial -> onDraw() (#" + redrawCount + ")" );
    //redrawCount++;
    
    // Determine the chart dimensions, if we haven't already done so: 
    if (drawingWidth == 0)
      {
      // **** Get actual layout parameters: ****
      drawingWidth = this.getWidth();
      drawingHeight = this.getHeight();
      
      // --DEBUG!--Log.i( UIActivity.APP_TAG, "       -> First Time: Set Bounds (" + drawingWidth + " x " + drawingHeight + ")" );

      
      //drawingTop = this.getTop();
      //drawingLeft = this.getLeft();
      // **** Calculate the locations of scale labels, etc: ****
      calcDial();

      // Paint for scale: 
      scalePaint.setColor(Color.BLACK);
      scalePaint.setTextSize(11);
      scalePaint.setTextAlign(Paint.Align.CENTER);
      scalePaint.setTypeface(Typeface.DEFAULT_BOLD);
      scalePaint.setAntiAlias(true);
      
      // Paint for needle: 
      needlePaint = needleDrawable.getPaint();
      needlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
      needlePaint.setStrokeWidth(3);
      needlePaint.setColor(0xA0F00000);

      needleShape = new PathShape(needlePath,drawingWidth,drawingHeight);
      }

    
    scalePaint.setTextSize(14);
    canvas.drawText( dialLabel, labelX, labelY, scalePaint);
    scalePaint.setTextSize(12);
    for (int n=0; n<numberDivisions; n++)
      {
      canvas.drawText(scaleLabels[n], slabelX[n], slabelY[n], scalePaint);
      }
    
        
    // *** Draw the needle: ****
    makeNeedle();
    
    needleDrawable.setBounds(0,0,drawingWidth,drawingHeight);
    needleDrawable.setShape(needleShape);
    needleDrawable.draw(canvas);
    
    }
  
  }  // [Class]
  

