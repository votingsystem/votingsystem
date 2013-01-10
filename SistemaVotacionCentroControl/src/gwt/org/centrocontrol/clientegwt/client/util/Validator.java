package org.centrocontrol.clientegwt.client.util;

import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.datepicker.client.DateBox;

public class Validator {

  /**
   * RFC 2822 compliant
   * http://www.regular-expressions.info/email.html
   */
  private final static String EMAIL_VALIDATION_REGEX = "[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?";

  public static boolean isValidEmail(String email) {
	  return email.matches(EMAIL_VALIDATION_REGEX);
  }
   
  public static boolean isSamePassword(PasswordTextBox password1, PasswordTextBox password2) {
	  return (password1.getText().equals(password2.getText()));
  }
  
  public static void validateNumber(String number) throws ValidatorException {    
	    if (!number.matches("\\+?[0-9]+")) {
	      throw new ValidatorException("Número no válido");
	    }
  }
 
  public static boolean isTextBoxEmpty(TextBox textBox) {   
	  return ("".equals(textBox.getText().trim()));
  }
  
  public static boolean isTextAreaEmpty(TextArea textArea) {   
	  return ("".equals(textArea.getText().trim()));
  }
  
  public static boolean isDateBoxEmpty(DateBox dateBox) {   
	  if(dateBox.getValue() == null) return true;
	  else return false; 
  }
  
  public static void setListBoxValue(ListBox listBox, Integer value) {   
	  if (value == null || "".equals(value)) return;
	  for (int i = 0; i < listBox.getItemCount(); i++) {
		  Integer itemValue = new Integer(listBox.getValue(i));
		  if(value.intValue() == itemValue.intValue()) {
			  listBox.setSelectedIndex(i);
			  break;
		  } 
	  }

  }
  
}