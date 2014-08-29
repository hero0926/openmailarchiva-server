
/* Copyright (C) 2005-2007 Jamie Angus Band 
 * MailArchiva Open Source Edition Copyright (c) 2005-2007 Jamie Angus Band
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation; either version
 * 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, see http://www.gnu.org/licenses or write to the Free Software Foundation,Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */


package com.stimulus.archiva.presentation;

import com.stimulus.archiva.domain.*;
import com.stimulus.archiva.domain.EmailFilter.Condition;
import com.stimulus.archiva.domain.Volume.Status;
import com.stimulus.archiva.domain.Volumes.AutoCreateEvent;
import com.stimulus.archiva.domain.fields.EmailField;
import com.stimulus.archiva.domain.fields.EmailFields;
import com.stimulus.archiva.exception.*;
import com.stimulus.archiva.security.realm.ADRealm;
import com.stimulus.archiva.service.*;
import com.stimulus.archiva.service.ConfigurationService.IAPTestStatus;
import com.stimulus.archiva.authentication.*;
import com.stimulus.struts.*;
import com.stimulus.util.Compare;
import com.stimulus.util.EnumUtil;
import org.apache.commons.logging.*;
import com.stimulus.archiva.log.*;
import java.io.Serializable;
import java.util.regex.*;
import java.util.*;
import javax.servlet.http.*;
import javax.mail.*;
import java.net.*;

public class ConfigBean extends BaseBean  implements HttpSessionBindingListener,Serializable {

  private static final long serialVersionUID = 2275642295115995805L;
  protected static Log logger = LogFactory.getLog(MessageBean.class.getName());
  protected static final Log audit = LogFactory.getLog("com.stimulus.archiva.audit");
  
  public static final String IPV4_REGEX = "\\A(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}\\z";
  public static final String IPV6_HEX4DECCOMPRESSED_REGEX = "\\A((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?) ::((?:[0-9A-Fa-f]{1,4}:)*)(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}\\z";
  public static final String IPV6_6HEX4DEC_REGEX = "\\A((?:[0-9A-Fa-f]{1,4}:){6,6})(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}\\z";
  public static final String IPV6_HEXCOMPRESSED_REGEX = "\\A((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)\\z";
  public static final String IPV6_REGEX = "\\A(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}\\z";

  protected String lookupPassword = "";
  protected String lookupUsername = "";
  protected String testAuthenticate = "";
  protected ChainedException.Level logLevel = ChainedException.Level.DEBUG;
  protected String recoveryOutput = "";
  protected boolean recoveryComplete = false;
  protected Config config = null;
  protected int recoveryFailed = 0;
  protected String mailboxTestOutput = "";
  protected String passPhrase1;
  protected String passPhrase2;
  protected String masterLoginPassword1;
  protected String masterLoginPassword2;
  protected String lookupAttribute;
  protected int lookupIndex;
  protected String lookupValue;
  protected ArrayList<String> ldapAttributes = new ArrayList<String>();
  protected String lookupError;
  protected String logFile;
  

public ConfigBean() {
  		
  }

  public Config getConfig() {
  	return config;
  }
  /* public List<EmailField> getFields() {
		 ArrayList<EmailField>  list = new ArrayList<EmailField>();
		 Iterator i = EmailField.getAvailableFields().values().iterator();
		 while (i.hasNext()) {
			 EmailField ef = (EmailField)i.next();
			 if (ef.isStored()) 
				 list.add(ef);			 
		 }
		 return list;
	 }
	 */
  public List<String> getRuleFields() {
	  	 ArrayList<String>  list = new ArrayList<String>();
	  	EmailFields emailFields = Config.getConfig().getEmailFields();
		 for (EmailField ef : emailFields.getAvailableFields().values()) {
			 if (!Compare.equalsIgnoreCase(ef.getName(),"body") &&
				 !Compare.equalsIgnoreCase(ef.getName(),"attachments"))	{
				 	list.add(ef.getName());			 
			 }
		 }
		 list.add("addresses");
		 Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
		 return list;
  }

  public List<String> getRuleFieldLabels() {
	  	 ArrayList<String>  list = new ArrayList<String>();
	  	 EmailFields emailFields = Config.getConfig().getEmailFields();
	  	 for (EmailField ef : emailFields.getAvailableFields().values()) {
	  		 if (!Compare.equalsIgnoreCase(ef.getName(),"body") &&
					 !Compare.equalsIgnoreCase(ef.getName(),"attachments"))	{ 
	  			 list.add(ef.getResource());			 
	  		 }
		 }
	  	 list.add("field_label_addresses");
		 Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
		 return translateList(list);
  }

  public List<String> getRuleActionFields() {
  	return EnumUtil.enumToList(ArchiveFilter.Action.values()); 
  }
  
  public List<String> getRuleActionLabels() {
	  return translateList(EnumUtil.enumToList(ArchiveFilter.Action.values(),"action_label_"));
  }
  
  public List<String> getRuleOperatorFields() { return EnumUtil.enumToList(EmailFilter.Operator.values()); };
	
  public List<String> getRuleOperatorLabels() {
	 return translateList(EnumUtil.enumToList(EmailFilter.Operator.values(),"config.sec_rules_operator_"));
   }
    
  public List<String> getRuleClauseConditionFields() { return EnumUtil.enumToList(EmailFilter.Condition.values()); };
	
  public List<String> getRuleClauseConditionLabels() {
	 return translateList(EnumUtil.enumToList(EmailFilter.Condition.values(),"config.sec_rules_clauses_condition_"));
   }

  public List<String> getMethods() {
  	return EnumUtil.enumToList(Criteria.Method.values());  
  }
  
  public List<String> getMethodLabels() {
  	return translateList(EnumUtil.enumToList(Criteria.Method.values(),"methode_label_"));
  }
  
  public List<String> getFields() {
	
	  ArrayList<String> fieldList = new ArrayList<String>();
	  EmailFields emailFields = Config.getConfig().getEmailFields();
	  for (EmailField ef : emailFields.getAvailableFields().values()) {
		  fieldList.add(ef.getName());
	  }
	  fieldList.add("all");
	  fieldList.add("addresses");
	  Collections.sort(fieldList, String.CASE_INSENSITIVE_ORDER);
	  return fieldList;
  }
  
  public List<String> getFieldLabels() {
	  ArrayList<String> fieldLabelList = new ArrayList<String>();
	  EmailFields emailFields = Config.getConfig().getEmailFields();
	  for (EmailField ef :  emailFields.getAvailableFields().values()) {
		  fieldLabelList.add(ef.getResource().toLowerCase(Locale.ENGLISH));
	  }
	  fieldLabelList.add("field_label_all");
	  fieldLabelList.add("field_label_addresses");
	  Collections.sort(fieldLabelList, String.CASE_INSENSITIVE_ORDER);
  	  return translateList(fieldLabelList,true); 
  }
  
  public List<String> getOperators() {
	return EnumUtil.enumToList(Criteria.Operator.values());  
  }
  
  public List<String> getOperatorLabels() {
	return translateList(EnumUtil.enumToList(Criteria.Operator.values(),"operator_"));
  }
  

  public List<String> getADRoleMapAttributes() {
	return ADIdentity.ATTRIBUTES;
  }

  public List<String> getADRoleMapAttributeLabels() {
    return translateList(ADIdentity.ATTRIBUTE_LABELS);
  }
 

  public List<String> getDebugLoggingLevelLabels() {
	  List<String> logLvl = new ArrayList<String>();
	  List<String> loggingLevels = EnumUtil.enumToList(ChainedException.Level.values());
	  for (String logLevel : loggingLevels) {
		  logLvl.add("config.log_level_"+logLevel.toString().toLowerCase(Locale.ENGLISH));
	  }
	  return translateList(Collections.unmodifiableList(logLvl));
  }

  public List<String> getDebugLoggingLevels() {
	  return EnumUtil.enumToList(ChainedException.Level.values());
  }
  
  public void setDebugLoggingLevel(String level) {
	  logLevel = ChainedException.Level.valueOf(level.toUpperCase(Locale.ENGLISH));
  }
  
  public String getDebugLoggingLevel() {
	  return logLevel.toString().toLowerCase(Locale.ENGLISH);
  }

  
  public boolean getAllowMasterPasswordEntry() {
	  MailArchivaPrincipal cp = (MailArchivaPrincipal)ActionContext.getActionContext().getRequest().getUserPrincipal();
	  return cp.getRole().equals("master") || (cp.getRole().equals("administrator") && config.getAuthentication().isLegacyMasterPassword());
	  
  }
  
  public void setArchiveInbound(boolean archiveInbound) { config.getArchiveFilter().setArchiveInbound(archiveInbound); };

  public void setArchiveOutbound(boolean archiveOutbound) {  config.getArchiveFilter().setArchiveOutbound(archiveOutbound);  };

  public void setArchiveInternal(boolean archiveInternal) { config.getArchiveFilter().setArchiveInternal(archiveInternal); };

  public void setArchiveOther(boolean archiveOther) { config.getArchiveFilter().setArchiveOther(archiveOther); };

  public boolean getArchiveInbound() {  return config.getArchiveFilter().getArchiveInbound(); }

  public boolean getArchiveOutbound() {return config.getArchiveFilter().getArchiveOutbound(); }

  public boolean getArchiveInternal() { return config.getArchiveFilter().getArchiveInternal(); }

  public boolean getArchiveOther() { return config.getArchiveFilter().getArchiveOther(); }


  public void setIndexAttachments(boolean indexAttachments) { config.getIndex().setIndexAttachments(indexAttachments); };
  
  public boolean getIndexAttachments() { return config.getIndex().getIndexAttachments(); }
  
  public void setIndexMessageBody(boolean indexMessageBody) { config.getIndex().setIndexMessageBody(indexMessageBody); };
  
  public boolean getIndexMessageBody() { return config.getIndex().getIndexMessageBody(); }
  
  public void setMaxMessageSize(int maxMessageSize) { config.getArchiver().setMaxMessageSize(maxMessageSize); }
  
  public int getMaxMessageSize() { return config.getArchiver().getMaxMessageSize(); }
  
 
  public int getIndexThreads() { return config.getIndex().getIndexThreads(); }
  
  public void setIndexThreads(int indexThreads) { config.getIndex().setIndexThreads(indexThreads); }
  
  public List<String> getIndexLanguages() {
      List<String> labels = new ArrayList<String>();
      for (Map.Entry<String,String> searchAnalyzer : config.getSearch().getSearchAnalyzers().entrySet()) {
    	  labels.add((String)searchAnalyzer.getKey());
      }
      return labels;
  }
  
  public List<String> getIndexLanguageLabels() {
	  List<String> labels = new ArrayList<String>();
      for (Map.Entry<String,String> searchAnalyzer : config.getSearch().getSearchAnalyzers().entrySet()) {
    	  labels.add("searchresults.language_"+searchAnalyzer.getKey());
      }
	  return translateList(labels);
  }
  
  public void setIndexLanguageDetection(boolean indexLanguageDetection) { config.getIndex().setIndexLanguageDetection(indexLanguageDetection); }
  
  public boolean getIndexLanguageDetection() {  return config.getIndex().getIndexLanguageDetection();  }
  
  public void setIndexLanguage(String language) { config.getIndex().setIndexLanguage(language); }
  
  public String getIndexLanguage() { return config.getIndex().getIndexLanguage(); }
  
  public void setAuthMethod(String authMethod) { config.getAuthentication().setAuthMethod(authMethod); }
  
  public String getAuthMethod() { return config.getAuthentication().getAuthMethod().toString().toLowerCase(Locale.ENGLISH); }

  public void setDefaultLoginDomain(String defaultLoginDomain) { config.getAuthentication().setDefaultLoginDomain(defaultLoginDomain); }
  
  public String getDefaultLoginDomain() { return config.getAuthentication().getDefaultLoginDomain(); };
  
  public List<String> getAuthMethods() {
  	return EnumUtil.enumToList(Authentication.AuthMethod.values());
  }
  
  public List<String> getAuthMethodLabels() {
  	return translateList(EnumUtil.enumToList(Authentication.AuthMethod.values(),"config.sec_auth_method_"));
  }
 

  public String getInitialSortField() {
	  return config.getSearch().getInitialSortField().toLowerCase(Locale.ENGLISH);
  }
  
  public void setInitialSortField(String sortField) {
	  config.getSearch().setInitialSortField(sortField.toLowerCase(Locale.ENGLISH));
  }
  public String getInitialSortOrder() {
      return config.getSearch().getInitialSortOrder().toString().toLowerCase(Locale.ENGLISH);
  }
  
  public void setInitialSortOrder(String sortOrder) {
	  config.getSearch().setInitialSortOrder(Search.SortOrder.valueOf(sortOrder.toUpperCase(Locale.ENGLISH)));
  }
  
  public List<String> getInitialSortFields() {
	  return getRuleFields();
  }
  
  public List<String> getInitialSortFieldLabels() {
	  return getRuleFieldLabels();
  }
  
  public List<String> getInitialSortOrders() {
  	return EnumUtil.enumToList(Search.SortOrder.values());
  }
  
  public List<String> getInitialSortOrderLabels() {
  	return translateList(EnumUtil.enumToList(Search.SortOrder.values(),"config.gen_search_initial_sort_order_"));
  }
  
  
  public void setInitialDateType(String dateType) {
	  config.getSearch().setInitialDateType(Search.DateType.valueOf(dateType.toUpperCase(Locale.ENGLISH)));
  }
  
  public String getInitialDateType() {
	  return config.getSearch().getInitialDateType().toString().toLowerCase(Locale.ENGLISH);
  }
  
  public List<String> getInitialDateTypes() {
  	return EnumUtil.enumToList(Search.DateType.values());
  }
  
  public List<String> getInitialDateTypeLabels() {
  	return translateList(EnumUtil.enumToList(Search.DateType.values(),"config.gen_search_initial_date_type_"));
  }
	  
  public void setOpenIndex(String openIndex) {
	  Search.OpenIndex openIdx = Search.OpenIndex.SEARCH;
	  try { 
		  openIdx = Search.OpenIndex.valueOf(openIndex.trim().toUpperCase(Locale.ENGLISH));
	  } catch (IllegalArgumentException iae) {
  		logger.error("failed to apply open index type to search. open index type is set to an illegal value {type='"+openIndex+"'}");
	}
	  config.getSearch().setOpenIndex(openIdx);
  }
  
  public String getOpenIndex() {
      return config.getSearch().getOpenIndex().toString().toLowerCase(Locale.ENGLISH);
  }
  
  public List<String> getOpenIndexes() {
	  return EnumUtil.enumToList(Search.OpenIndex.values());
  }
  public List<String> getOpenIndexLabels() {
	  return translateList(EnumUtil.enumToList(Search.OpenIndex.values(),"config.gen_search_open_index_per_"));
  }
  
	public boolean getAutoCreateVolume() {
		return config.getVolumes().getAutoCreate();
	}

	public void setAutoCreateVolume(boolean autoCreateVolume) {
		config.getVolumes().setAutoCreate(autoCreateVolume);
	}
	
	public String getAutoCreateEvent() {
		return config.getVolumes().getAutoCreateEvent().name();
	}

	public void setAutoCreateEvent(String autoCreateEventName) {
		Volumes.AutoCreateEvent autoCreateEvent = Volumes.AutoCreateEvent.valueOf(autoCreateEventName);
		config.getVolumes().setAutoCreateEvent(autoCreateEvent);
	}
	
	
	public AutoCreateEvent[] getAutoCreateEvents() {
		return Volumes.AutoCreateEvent.values();
	}
  
  public void reset() {
  	
  }

  public String reload() {
	  initLdapLookup();
      testAuthenticate = "";
      recoveryOutput = "";
      recoveryComplete = false;
      recoveryFailed = 0;
      return "reload";
  }
  
  /* agent ip addresses */
  
  public List<String> getAgentIPAddresses() {
	  return config.getAgent().getIPAddresses();
  }
  
  public String getAgentIPAddress(int index) {
	  return config.getAgent().getIPAddress(index);
  }
  
  public void setAgentIPAddress(int index, String value) {
	  config.getAgent().setIPAddress(index, value);
  }
  public String newAgentIPAddress() {
	  config.getAgent().addAllowedIPAddress("127.0.0.1");
	  return "reload";
  }
  
  public String deleteAgentIPAddress(int id) {
	  config.getAgent().removeIPAddress(id);
	  return "reload";
  }
  
  public String getAgentSMTPPort() {
	  return Integer.toString(config.getSMTPServerService().getSMTPPort());
  }
  
  public String getAgentSMTPAddress() {
	  return config.getSMTPServerService().getIpAddress();
  }
  
  public boolean getAgentSMTPEnable() {
	  return config.getSMTPServerService().getSMTPEnable();
  }
  
  public void setAgentSMTPPort(String port) {
	  int portVal = Integer.valueOf(port);
	  config.getSMTPServerService().setSMTPPort(portVal);
  }
  
  public void setAgentSMTPAddress(String address) {
	  config.getSMTPServerService().setIpAddress(address);
  }
  
  public void setAgentSMTPAuth(boolean auth) {
	  config.getSMTPServerService().setSMTPAuth(auth);
  }
  
  public boolean getAgentSMTPAuth() {
	  return config.getSMTPServerService().getSMTPAuth();
  }
  
  public void setAgentSMTPPassword(String smtpPassword) {
	  config.getSMTPServerService().setSMTPPassword(smtpPassword);
  }
  
  public String getAgentSMTPPassword() {
	  return config.getSMTPServerService().getSMTPPassword();
  }
  
  public void setAgentSMTPUsername(String username) {
	  config.getSMTPServerService().setSMTPUsername(username);
  }
  
  public String getAgentSMTPUsername() {
	  return config.getSMTPServerService().getSMTPUsername();
  }
  
  public void setAgentSMTPTLS(boolean smtpTLS) {
	  config.getSMTPServerService().setSMTPTLS(smtpTLS);
  }
  
  public boolean getAgentSMTPTLS() {
	  return config.getSMTPServerService().getSMTPTLS();
  }
  
  public boolean getAgentMilterEnable() {
	  return config.getMilterServerService().getEnable();
  }
  
  public void setAgentMilterEnable(boolean enable) {
	  config.getMilterServerService().setEnable(enable);
  }
  
 
  
  public void setAgentSMTPEnable(boolean enable) {
	  config.getSMTPServerService().setSMTPEnable(enable);
  }
  
  public String getAgentMilterPort() {
	  return Integer.toString(config.getMilterServerService().getPort());
  }
  
  public String getAgentMilterAddress() {
	  return config.getMilterServerService().getIpAddress();
  }
  
  public void setAgentMilterPort(String port) {
	  int portVal = Integer.valueOf(port);
	  config.getMilterServerService().setPort(portVal);
	  
  }
  
  public void setAgentMilterAddress(String address) {
	  config.getMilterServerService().setIpAddress(address);
  }
  /* volumes */
  
  public List<VolumeBean> getVolumes() {
	  try {
		  return VolumeBean.getVolumeBeans(config.getVolumes());
	  } catch (Throwable t) {
		  System.out.println("error:"+t.getMessage());
		  t.printStackTrace();
		  return null;
	  }
  }
  
  public Volume getMessageStoreVolume(int index) {
	  	return config.getVolumes().getVolume(index);
  }

  public String indexVolume(int volumeIndex) {
      logger.debug("indexVolume() {volumeIndex='"+volumeIndex+"'}");
      try {
    	  MessageService.indexVolume(getMailArchivaPrincipal(),volumeIndex);
      } catch (Exception e) {
    	  setSimpleMessage(getMessage("config.error_volume_index")+":"+e.getMessage());
      }
      return "reload";
  }

  public String closeVolume(int volumeIndex) {
      logger.debug("closeVolume()");
      try {
    	  config.getVolumes().closeVolume(volumeIndex);
      } catch (Exception e) {
		  setSimpleMessage(getMessage("config.error_volume_close")+":"+e.getMessage());
		  return "reload";
	  }
      setSimpleMessage(getMessage("config.volume_status_change_notification"));
     return "reload";
  }
  
   public String unmountVolume(int volumeIndex) {
	  logger.debug("unmountVolume()");
	  try {
		  config.getVolumes().unmountVolume(volumeIndex);
	  } catch (Exception e) {
		  setSimpleMessage(getMessage("config.error_volume_unmount")+":"+e.getMessage());
		  return "reload";
	  }
      setSimpleMessage(getMessage("config.volume_status_change_notification"));
      return "reload";
  }

  public String deleteVolume(int id) {
    logger.debug("deleteVolume() {volumeIndex='"+id+"'}");
    try {
    	config.getVolumes().removeVolume(id);
    } catch (Exception e) {
		  setSimpleMessage(getMessage("config.error_volume_delete")+":"+e.getMessage());
		  return "reload";
	}
    setSimpleMessage(getMessage("config.volume_status_change_notification"));
  	return "reload";
  }
  public String newVolume() {
  	logger.debug("newVolume()");
  	try {
  		config.getVolumes().newVolume();
  	 } catch (Exception e) {
		  setSimpleMessage(getMessage("config.error_volume_create")+":"+e.getMessage());
		  return "reload";
	}	
  	setSimpleMessage(getMessage("config.volume_status_change_notification"));
  	return "reload";
  }

  public String prioritizeVolume(int id)  {
  	config.getVolumes().setVolumePriority(id, Volumes.Priority.PRIORITY_HIGHER);
  	setSimpleMessage(getMessage("config.volume_status_change_notification"));
  	return "reload";
  }
  public String dePrioritizeVolume(int id) {
  	config.getVolumes().setVolumePriority(id, Volumes.Priority.PRIORITY_LOWER);
  	setSimpleMessage(getMessage("config.volume_status_change_notification"));
  	return "reload";
  }

  /* domains */

  public String newDomain() throws ConfigurationException {
    	config.getDomains().addDomain();
    	return "reload";
  }

  public String deleteDomain(int id) throws ConfigurationException {
    	config.getDomains().deleteDomain(id);
    	return "reload";
  }
  
  public List<DomainBean> getDomains() {
  	return DomainBean.getDomainBeans(config.getDomains().getDomains());
  }

  public List<String> getDomainLabels() {
	  List<String> domainList = new LinkedList<String>();
      List<Domains.Domain> domains = config.getDomains().getDomains();
      for (Domains.Domain domain : domains) {
    	  domainList.add(domain.getName());
      }
      return domainList;
  }

  /* ad role maps */
  
  public String deleteAdRoleMap(int id) {
      config.getADIdentity().deleteRoleMap(id);
      return "reload";
  }

  public String newAdRoleMap() throws ConfigurationException {
	  	config.getADIdentity().newRoleMap();
    	return "reload";
  }
  
  public Identity.RoleMap getAdRoleMap(int index) {
    	return config.getADIdentity().getRoleMap(index);
  }

  public List<LDAPRoleMapBean> getADRoleMaps() {
      return LDAPRoleMapBean.getLDAPRoleMapBeans(config.getADIdentity().getRoleMaps());
  }
  
 
 
  public ADIdentity getADIdentity() {
	  return config.getADIdentity();
  }
 
  /* basic role map */
  
/* ad role maps */
  
  public String deleteBasicRoleMap(int id) {
      config.getBasicIdentity().deleteRoleMap(id);
      return "reload";
  }

  public String newBasicRoleMap() throws ConfigurationException {
	  	config.getBasicIdentity().newRoleMap();
    	return "reload";
  }
  
  public Identity.RoleMap getBasicRoleMap(int index) {
    	return config.getBasicIdentity().getRoleMap(index);
  }

  public List<BasicRoleMapBean> getBasicRoleMaps() {
      return BasicRoleMapBean.getBasicRoleMapBeans(config.getBasicIdentity().getRoleMaps());
  }
  
  /* archive rules */

  public String deleteArchiveRule(int id) throws ConfigurationException {
  	config.getArchiveFilter().deleteArchiveRule(id);
  	return "reload";
  }
  
  public String newArchiveRule() throws ConfigurationException {
  	config.getArchiveFilter().newArchiveRule();
  	return "reload";
  }

  
  public String prioritizeArchiveRule(int id) throws ConfigurationException {
  	config.getArchiveFilter().setPriority(id, EmailFilter.Priority.HIGHER);
  	return "reload";
  }
  public String dePrioritizeArchiveRule(int id) throws ConfigurationException {
  	config.getArchiveFilter().setPriority(id, EmailFilter.Priority.LOWER);
  	return "reload";
  }

  public String newArchiveRuleClause(int id) {
	  EmailFilter.FilterRule filterRule = config.getArchiveFilter().getArchiveRule(id);
	  EmailFilter.FilterClause clause = new EmailFilter.FilterClause("subject", Condition.CONTAINS,"");
	  filterRule.addClause(clause);
	return "reload";
 }

  public String deleteArchiveRuleClause(int id, int cid ) {
	  EmailFilter.FilterRule filterRule = config.getArchiveFilter().getArchiveRule(id);
	// we do not want to allow deletion 
	if (filterRule.getFilterClauses().size()<2)
		return "reload";

	EmailFilter.FilterClause clause = filterRule.getClause(cid);
	if (clause!=null)
		filterRule.deleteClause(clause);
	return "reload";
  }

  public List<ArchiveRuleBean> getArchiveRules() {
  	return ArchiveRuleBean.getArchiveRuleBeans(config.getArchiveFilter().getArchiveRules());
  }

  public ArchiveRuleBean getArchiveRule(int index) {
  	return new ArchiveRuleBean(config.getArchiveFilter().getArchiveRule(index));
  }

  public void setPassPhrase(String passPhrase) {
      //logger.debug("setPassPhrase {passPhrase='"+passPhrase+"'}");
      if (passPhrase.trim().length()>0)
    	  passPhrase1=passPhrase;
      
  }
  
  public void setPassPhraseAgain(String passPhrase) {
      //logger.debug("setPassPhrase {passPhrase='"+passPhrase+"'}");
      if (passPhrase.trim().length()>0)
    	  passPhrase2=passPhrase;
  }
  
  public boolean getDefaultPassPhraseModified() {
      return config.getArchiver().isDefaultPassPhraseModified();
  }
  
  public void setMasterLoginPassword(String masterLoginPassword) throws ArchivaException  {
	  if (masterLoginPassword.trim().length()>0)
    	  masterLoginPassword1=masterLoginPassword;
      
  }
  
  public void setMasterLoginPasswordAgain(String masterLoginPassword) throws ArchivaException  {
	  if (masterLoginPassword.trim().length()>0)
    	  masterLoginPassword2=masterLoginPassword;
  }
  
  public boolean getDefaultMasterLoginPasswordModified() {
      return config.getAuthentication().isDefaultMasterLoginModified();
  }
  
  public String getLookupPassword() {
      return lookupPassword;
  }
  
  public String getLookupUsername() {
      return lookupUsername;
  }
  public void setLookupPassword(String password) {
      this.lookupPassword = password;
  }
  
  public void setLookupUsername(String username) {
      this.lookupUsername = username;
  }
  
  public void setTestAuthenticate(String testAuthenticate) {
      this.testAuthenticate = testAuthenticate;
  }
  
  public String getLookupAttribute() {
	  return lookupAttribute;
  }
  public void setLookupAttribute(String lookupAttribute) {
	  this.lookupAttribute = lookupAttribute;
  }
  
  public int getLookupIndex() {
	  return lookupIndex;
  }
  
  public void setLookupIndex(int lookupIndex) {
	  this.lookupIndex = lookupIndex;
  }
  
  public String getLookupValue() {
	  return lookupValue; 
  }
  
  public void setLookupValue(String lookupValue) {
	  this.lookupValue = lookupValue;
  }
  
  public String getTestAuthenticate() {
      return testAuthenticate;
  }
  
  public int getPollingIntervalSecs() {
	  return config.getMailboxConnections().getPollingIntervalSecs();
  }

  public void setPollingIntervalSecs(int pollingIntervalSecs) {
	  config.getMailboxConnections().setPollingIntervalSecs(pollingIntervalSecs);
  }
  
  public MailboxConnectionBean getMailboxConnection() {
	  return new MailboxConnectionBean(config.getMailboxConnections().getConnection());
  }
  
  public List<String> getProtocols() {
  	return EnumUtil.enumToList(MailboxConnections.Protocol.values());
  }
  
  public List<String> getProtocolLabels() {
  	return translateList(EnumUtil.enumToList(MailboxConnections.Protocol.values(),"config.mailbox_connections_protocol_"));
  }
  
  public List<String> getConnectionModes() {
	  return EnumUtil.enumToList(MailboxConnections.ConnectionMode.values());
  }
  
  public List<String> getConnectionModeLabels() {
	  	return translateList(EnumUtil.enumToList(MailboxConnections.ConnectionMode.values(),"config.mailbox_connections_connection_mode_"));
  }
  
  
 
 
  public String configurationform() {
	  
	  try {
		  
		  logger.debug("config clone");
		  config = Config.getConfig().clone(getMailArchivaPrincipal());
	  		
	  	} catch (ConfigurationException ce) {
	 		logger.error("failed to load configuration",ce);
	 		throw new ChainedRuntimeException(ce.toString(),ce,logger);
	 	}
	  	try {
	  		config.getVolumes().loadAllVolumeInfo();
	  	} catch (ConfigurationException ce) { 
	  		logger.error("failed to load volume info:"+ce.getMessage());
	  	}
	  logLevel = ConfigurationService.getLoggingLevel();
	  
	  if (getNoWaitingMessagesInNoArchiveQueue()>0) {
	  		if (getServlet()!=null)
	  			setSimpleMessage(getMessage("config.no_archive_warning")+" "+getNoWaitingMessagesInNoArchiveQueue()+".");
		}

		if (getDefaultPassPhraseModified()) {
				passPhrase1 = passPhrase2 = "modified";
		} else {
			passPhrase1 = "changeme1";
			passPhrase2 = "changeme2";
		}
		if (getDefaultMasterLoginPasswordModified()) {
			masterLoginPassword1 = masterLoginPassword2 = "modified";
		} else {
			masterLoginPassword1 = "admin";
			masterLoginPassword2 = "admin";
		}
	  	return "success";
  }
  
  public String recoverEmails() {
	  recoveryComplete = false;
	  recoveryOutput = "";
	  recoveryFailed = 0;
	  MessageService.recoverNoArchiveMessages(new RecoverInfo());
	  return "reload";
  }
  
  public String recover() {
	 
	  return "success";
  }
  
  
  public String getRecoveryOutput() {
	  return recoveryOutput;
  }

  public boolean getRecoveryComplete() {
	  return recoveryComplete;
  }
  
  public int getRecoveryFailed() {
	  return recoveryFailed;
  }
  
  public int getNoWaitingMessagesInNoArchiveQueue() {
	  	return MessageService.getNoMessagesForRecovery();
  }
  
  public String quarantine() {
	  MessageService.quarantineMessages();
	  return "success";
  }
  
  public int getNoQuarantinedEmails() {
	  return MessageService.getNoQuarantinedMessages();
  }
  
  
  public class MailboxConnectionTestStatus extends IAPTestStatus {
	  
	  public void statusUpdate(String result) {
		  mailboxTestOutput += result + "<br>";
	  }
	  
  }

  public String getMailboxTestOutput() {
	  return mailboxTestOutput;
  }
	  
  public String testMailboxConnection() {
	  mailboxTestOutput = "please wait. testing mailbox connection...<br>";
	  MailboxConnection connection = config.getMailboxConnections().getConnection();
	  if (connection!=null) {
		  ConfigurationService.testMailboxConnection(connection,new MailboxConnectionTestStatus());
	  }
	  return "reload";
  }

  
  public String testloginform() {
	  testAuthenticate = "";
	  config.getConfig().getADIdentity().saveHostsFileEntry();
	  return "success";
  }
  
  
  public void initLdapLookup() {
	  ldapAttributes = new ArrayList<String>();
      lookupError = "";
      lookupAttribute = "";
	  lookupValue = "";
	  config.getConfig().getADIdentity().saveHostsFileEntry();
  }
  public String lookuprolecriterionldapform() {
	  initLdapLookup();
	  return "success";
  }
  
  public String lookuprolecriterionform() {
	  initLdapLookup();
	  return "success";
  }
  
 
  public String configure() throws ArchivaException
  {
	 Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
    SubmitButton button = getSubmitButton();
    
    if (button==null | button.action==null)
        return "reload";
    
  	logger.debug("configure() {action ='"+button.action+"', value='"+button.value+"'}");

	  	if (button.action.equals("newvolume")) {
	  		return newVolume(); 
		} else if (button.action.equals("deletevolume")) {
	  		return deleteVolume(Integer.parseInt(button.value));
	  	} else if (button.action.equals("prioritizevolume")) {
	  		return prioritizeVolume(Integer.parseInt(button.value));
	  	} else if (button.action.equals("deprioritizevolume")) {
	  		return dePrioritizeVolume(Integer.parseInt(button.value));
	  	} else if (button.action.equals("newarchiverule")) {
	  		return newArchiveRule();
		} else if (button.action.equals("deletearchiverule")) {
			return deleteArchiveRule(Integer.parseInt(button.value));
		} else if (button.action.equals("prioritizearchiverule")) {
	  		return prioritizeArchiveRule(Integer.parseInt(button.value));
	  	} else if (button.action.equals("deprioritizearchiverule")) {
	  		return dePrioritizeArchiveRule(Integer.parseInt(button.value));
	  	} else if (button.action.equals("cancel")) {
	  		return cancel();
	  	} else if (button.action.equals("indexvolume")) {
	  	    return indexVolume(Integer.parseInt(button.value));
	  	} else if (button.action.equals("newadrolemap")) {
	  	    return newAdRoleMap();
	  	} else if (button.action.equals("deleteadrolemap")) {
	  	    return deleteAdRoleMap(Integer.parseInt(button.value));
	  	} else if (button.action.equals("newbasicrolemap")) {
	  	    return newBasicRoleMap();
	  	} else if (button.action.equals("deletebasicrolemap")) {
	  	    return deleteBasicRoleMap(Integer.parseInt(button.value));
	  	} else if (button.action.equals("newdomain")) {
	  	    return newDomain();
	  	} else if (button.action.equals("deletedomain")) {
	  	    return deleteDomain(Integer.parseInt(button.value));
	  	} else if (button.action.equals("closevolume")) {
	  	    return closeVolume(Integer.parseInt(button.value));
	  	} else if (button.action.equals("unmountvolume")) {
	  	    return unmountVolume(Integer.parseInt(button.value));
	  	} else if (button.action.equals("newagentipaddress")) {
	  		return newAgentIPAddress();
	  	} else if (button.action.equals("deleteagentipaddress")) {
	  		return deleteAgentIPAddress(Integer.parseInt(button.value));
	  	} else if (button.action.equals("reload")) {
	  	    return reload();
	  	} else if (button.action.equals("recoveremails")) {
	  		return recoverEmails();
	  	} else if (button.action.equals("quarantineemails")) {
	  		return quarantine();
	  	} else if (button.action.equals("testmailboxconnection")) {
	  		return testMailboxConnection();
	  	} else if (button.action.equals("newarchiveruleclause")) {
	  		return newArchiveRuleClause(Integer.parseInt(button.value));
	  	} else if (button.action.equals("deletearchiveruleclause")) {
	  		int dotpos = button.value.indexOf('.');
	  		String id = button.value.substring(0,dotpos);
	  		String cid = button.value.substring(dotpos+1,button.value.length());
	  		return deleteArchiveRuleClause(Integer.parseInt(id),Integer.parseInt(cid));
	  	} else if (button.action.equals("downloadlog")) {
	  		setLogFile(button.value);
	  		return "downloadlog";
	  	} else if (button.action.equals("deletelog")) {
	  		return deleteLog(button.value);
	  	}
	  		
	  	ActionContext ctx = ActionContext.getActionContext();
	 
	  	if (ctx.isSimpleErrorsExist())
	  		return reload();
	  	
	  	if (!createVolumeDirectories())
	  		return reload();
	
	 	boolean error = false;
	 
	  	if (!error) {
	  		setSimpleMessage(getMessage("config.saved"));
	  	}
	  	
		
	  	MailArchivaPrincipal principal = getMailArchivaPrincipal();
	  	
	  
	  	config.saveSettings(principal,true);
	  	
	  
	  	config.loadSettings(principal);
	  	Config.getConfig().loadSettings(principal);
	  	Config.getConfig().getServices().reloadConfigAll();
	  
	  	MessageService.init(); // initialize cipher keys (for new password)
	  	ConfigurationService.setLoggingLevel(logLevel);
	 
		return save();
  }
  
	
  
  protected boolean createVolumeDirectories() {
	      boolean success = true;
	      ActionContext ctx = ActionContext.getActionContext();
	      int c = 0;
	      for (Volume v : config.getVolumes().getVolumes()) {
	          if (!v.isEjected() && !MessageService.prepareVolume(v)) {
	        	  ctx.addSimpleError(getMessage("config.volume_create_failed")+" "+c+".");
	        	  success=false;
	          }
	          c++;
	      }
	      return success;
  }

  public String lookuprolecriterion() {
	 
	  SubmitButton button = getSubmitButton();
      String action = button.action;
      initLdapLookup();
      if (action!=null && Compare.equalsIgnoreCase(action, "lookup")) {    	 
	      try {
	    	  ldapAttributes = getAttributeValues(config.getADIdentity());
	      } catch (ArchivaException ae) {
	    	  lookupError = getMessage("lookup_role.failure")+":";
	          String message = ae.getMessage();
	        
	          if (message!=null) {
	        	  int infoIndex = message.lastIndexOf("{");
	        	  if (infoIndex>-1) {
	        		  message = message.substring(0,infoIndex-1);
	        	  }
	        	  lookupError= message + " " + getAuthenticationErrorHint(message);
	          }
	         return "success";
	      }
      } 
      return "success";
  }
  
  
  protected String getAuthenticationErrorHint(String error) {
	  String retError = "";
	  if (error.contains("(7)"))
		  retError += getMessage("config.sec_ad_7");
      if (error.contains("(37)"))
    	  retError += getMessage("config.sec_ad_37");
      if (error.contains("(24)")) 
    	  retError += getMessage("config.sec_ad_24");
      if (error.contains("(18)")) 
    	  retError += getMessage("config.sec_ad_18");
      if (error.contains("(14)"))
    	  retError += getMessage("config.sec_ad_14");
      if (error.contains("(906)"))
    	  retError += getMessage("config.sec_ad_906");
      if (error.contains("(68)"))
    	  retError += getMessage("config.sec_ad_68");
      if (error.contains("(6)"))
    	  retError += getMessage("config.sec_ad_6");
      if (error.contains("Invalid argument"))
    	  retError += getMessage("config.sec_ad_invalidargument");
      return retError;
  }
  
  protected ArrayList<String> getAttributeValues(ADIdentity identity) throws ArchivaException {
      ArrayList<String> ldapAttributes = new ArrayList<String>();
      ArrayList<ADRealm.AttributeValue> attributeValues;
	  attributeValues = ConfigurationService.getLDAPAttributeValues(config,identity,lookupUsername, lookupPassword);
      for (ADRealm.AttributeValue attributeValue: attributeValues) {
	    		 ldapAttributes.add(attributeValue.getAttribute()+" : "+attributeValue.getValue());  
      } 
      return ldapAttributes;
  }
  
  
  public String testlogin() {
      testAuthenticate = ConfigurationService.testAuthenticate(config,lookupUsername,lookupPassword);
      testAuthenticate = testAuthenticate+" "+getAuthenticationErrorHint(testAuthenticate);
      return "success";
  }
  

  public String save() {
    return "save";
  }

  public String cancel() {
      try {	  	
            config.loadSettings(getMailArchivaPrincipal());
            setSimpleMessage(getMessage("config.cancelled"));
      } catch (ConfigurationException ce) {
        logger.error("failed to load configuration",ce);
        throw new ChainedRuntimeException(ce.toString(),ce,logger);
      }
      reload();
      reset();
      return "cancel";
  }


  public void validate() {
      SubmitButton button = getSubmitButton();
      String action = button.action;
      if (action!=null && Compare.equalsIgnoreCase(action, "save")) {
	      ActionContext ctx = ActionContext.getActionContext();
	      
	      validateRequiredField(masterLoginPassword1, getMessage("config.sec_master_login_passwd_not_entered"));
	      if (Compare.equalsIgnoreCase(masterLoginPassword1,"admin")) {
	    	  ctx.addSimpleError(getMessage("config.sec_master_login_passwd_not_entered"));
	      } else {
		      if (!Compare.equalsIgnoreCase(masterLoginPassword1,masterLoginPassword2)) 
		    	  ctx.addSimpleError(getMessage("config.sec_master_login_passwd_not_match"));
		      else if (!masterLoginPassword1.equalsIgnoreCase("modified")) {
		    	  config.getAuthentication().setMasterLoginPassword(masterLoginPassword1);
		    	  masterLoginPassword1 = masterLoginPassword2 = "modified";
		      }
	      }
	      
	      
	      validateRequiredField(passPhrase1, getMessage("config.sec_enc_passwd_not_entered"));
	    
	      if (Compare.equalsIgnoreCase(passPhrase1,"changeme1")) {
	    	  ctx.addSimpleError(getMessage("config.sec_enc_passwd_not_entered"));
	      } else {
		      if (!Compare.equalsIgnoreCase(passPhrase1,passPhrase2)) 
		    	  ctx.addSimpleError(getMessage("config.sec_enc_passwd_not_match"));
		      else if (!passPhrase1.equalsIgnoreCase("modified")) {
		    	  config.getArchiver().setPassPhrase(passPhrase1);
		    	  passPhrase1 = passPhrase2 = "modified";
		      }
	      }
	      
	      
	      int j = 0;
	      for (EmailFilter.FilterRule ar : config.getArchiveFilter().getArchiveRules()) {
	    	  ArchiveRuleBean arb = new ArchiveRuleBean(ar);
	          validateRequiredField(arb.getAction(), getMessage("config.sec_rules_action_missing")+" "+j+".");
	          validateRequiredField(arb.getOperator(), getMessage("config.sec_rules_operator_missing")+" "+j+".");
	          for (EmailFilter.FilterClause fc : ar.getFilterClauses()) {
	        	  validateRequiredField(fc.getField(), getMessage("config.sec_rules_clause_field_missing")+" "+j+".");
	        	  validateRequiredField(fc.getValue(), getMessage("config.sec_rules_clause_value_missing")+" "+j+".");
	        	  validateRequiredField(fc.getCondition().toString(), getMessage("config.sec_rules_clause_condition_missing")+" "+j+".");
	        	  
	        	  if (fc.getCondition()==EmailFilter.Condition.MATCHES) {
	        		  try {
	    	              Pattern p = Pattern.compile(fc.getValue());
	    	          } catch (Exception e) {
	    	              ctx.addSimpleError(getMessage("config.sec_rules_clause_value_invalid")+" "+j+".");
	    	          }
	        	  }
	          }
	          j++;
	      }
	      j = 0;
	      for (Volume v : config.getVolumes().getVolumes()) {
	          validateRequiredField(v.getPath(), getMessage("config.volume_store_path_missing")+" "+j+".");
	          validateRequiredField(v.getIndexPath(), getMessage("config.volume_index_path_missing")+" "+j+".");
	          if (Compare.equalsIgnoreCase(v.getPath().trim(),v.getIndexPath().trim())) {
	        		  ctx.addSimpleError(getMessage("config.volume_paths_match")+" "+j+".");
	          }
	          j++;
	      }
	      
	      // active directory
	      
	      if (this.config.getAuthentication().getAuthMethod()==Authentication.AuthMethod.ACTIVEDIRECTORY) {
	          validateRequiredField(config.getADIdentity().getKDCAddress(), getMessage("config.sec_kdc_missing"));
	          validateRequiredField(config.getADIdentity().getLDAPAddress(), getMessage("config.sec_ldap_missing"));
	          validateRequiredField(config.getADIdentity().getBaseDN(),getMessage("config.sec_ad_base_dn"));
	          validateRequiredField(config.getADIdentity().getServiceDN(),getMessage("config.sec_ad_service_username_missing"));
	          validateRequiredField(config.getADIdentity().getServicePassword(),getMessage("config.sec_ad_service_password_missing"));
	      }
	      
	      j = 0;
	      for (Identity.RoleMap rolemap : config.getADIdentity().getRoleMaps()) {
	      ADIdentity.LDAPRoleMap r = (ADIdentity.LDAPRoleMap)rolemap;
	          validateRequiredField(r.getRole(), getMessage("config.sec_role_no_select")+" "+j+".");
	          validateRequiredField(r.getAttribute(), getMessage("config.sec_ldap_no_select")+" "+j+".");
	          validateRequiredField(r.getRegEx(), getMessage("config.sec_role_match_crit_invalid")+" "+j+".");
	          try {
	              Pattern p = Pattern.compile(r.getRegEx());
	          } catch (Exception e) {
	              ctx.addSimpleError(getMessage("config.sec_role_match_crit_invalid")+" "+j+".");
	          }
	          j++;
	      }
	      
	    
	      
	      j = 0;
	      for (Identity.RoleMap rolemap : config.getBasicIdentity().getRoleMaps()) {
	    	  BasicIdentity.BasicRoleMap r = (BasicIdentity.BasicRoleMap)rolemap;
	          validateRequiredField(r.getRole(), getMessage("config.sec_role_no_select")+" "+j+".");
	          validateRequiredField(r.getUsername(), getMessage("config.sec_role_match_username_invalid")+" "+j+".");
	          validateRequiredField(r.getLoginPassword(), getMessage("config.sec_role_match_password_invalid")+" "+j+".");
	          
	          String username = r.getUsername();
		      int at = username.lastIndexOf('@');
		      if (at==-1) {
		    	   validateRequiredField(null, getMessage("config.sec_role_match_username_format_invalid")+" "+j+".");
		      }
	          j++;
	      }
	  

	      j = 0;
	      for (Domains.Domain d : config.getDomains().getDomains()) {
	          validateRequiredField(d.getName(), getMessage("config.sec_domain_missing")+" "+j+".");
	          j++;
	      }
	      
	      j = 0;
	      MailboxConnection mailbox = config.getMailboxConnections().getConnection();
	      if (mailbox.getEnabled()) {
	    	validateRequiredField(mailbox.getServerName(), getMessage("config.mailbox_server_missing"));
		  	validateRequiredField(mailbox.getUsername(), getMessage("config.mailbox_username_missing"));
		  	validateRequiredField(mailbox.getPassword(), getMessage("config.mailbox_password_missing"));
	      }
		  checkDomainDuplicate(ctx);
	    
      }
  }

  
  protected void validateIPAddress(String value, String errorMessage) {
	  	if (value == null || value.trim().length() < 1) {
	      ActionContext.getActionContext().addSimpleError(errorMessage);
	    } else {
	    	 boolean matched = Pattern.matches(IPV4_REGEX,value) || 
	    	 				   Pattern.matches(IPV6_HEX4DECCOMPRESSED_REGEX,value) ||
	    	 				   Pattern.matches(IPV6_6HEX4DEC_REGEX,value) ||
	    	 				   Pattern.matches(IPV6_HEXCOMPRESSED_REGEX,value) ||
	    	 				   Pattern.matches(IPV6_REGEX, value);
	    	 if (!matched) {
	    		 ActionContext.getActionContext().addSimpleError(errorMessage);
	    	 }
	    }
  }
  
  
  protected void checkDomainDuplicate(ActionContext ctx) {
	  for (Domains.Domain domaina : config.getDomains().getDomains()) {
    	  for (Domains.Domain domainb : config.getDomains().getDomains()) {
    		  if (domaina==domainb) continue; // objects are the same
    		  if (domaina.getName().equalsIgnoreCase(domainb.getName())) {
    			  ctx.addSimpleError(getMessage("config.domains_duplication_error"));
    			  return;
    		  }
    	  }
      }
  }
 
  public class RecoverInfo extends MessageService.Recovery {
	  
	  public RecoverInfo() {
		  recoveryOutput = "";
		  recoveryComplete = false;
		  recoveryFailed = 0;
	  }
	  
	  public void update(Email email, boolean success, String output) {
		  if (success)
			  recoveryOutput += email.getEmailId().getUniqueID()+" ok<br>";
		  else
			  recoveryOutput += email.getEmailId().getUniqueID()+" failed ("+output+")<br>";
	  }
	  
	  public void start() {
		  recoveryOutput += "message recovery initiated..<br>";
		  
	  };
	  	
	  public void end(int failed, int success, int total) { 
		  if (total<1) {
			  recoveryOutput += "there are no messages to process<br>";
		  } else {
			  recoveryOutput += "recovery complete (failed:"+failed+" success:"+success+" total:"+total+")<br>";
		  }
		  recoveryComplete = true;
		  recoveryFailed = failed;
	  }

  }

  
  public String getVersion() {
	  return Config.getConfig().getApplicationVersion();
  }
  
  public List<String> getLDAPAttributes() {
	  return ldapAttributes;
  }
  
  public int getLDAPAttributeListSize() {
	  return ldapAttributes.size();
  }
  public List<String> getLDAPAttributeLabels() {
	  ArrayList<String> ldapAttributeLabels = new ArrayList<String>();
	  for (String attributeValue: ldapAttributes) {
		  
		  String value = "";
		  
		  if (attributeValue.length() > 60) {
			  value = attributeValue.substring(0,60);
			  value += "..";  
		  } else {
			  value = attributeValue;
		  }
		  ldapAttributeLabels.add(value);  
		  
	  } 
	  return ldapAttributeLabels;
  }

  public String getLookupError() {
	  return lookupError;
  }
  
  public void setLDAPAttributes(ArrayList<String> ldapAttributes) {
	  this.ldapAttributes = ldapAttributes;
  }
  
  public int getIndexMaxSize() { 
	  return config.getIndex().getIndexMaxSize();
  }
  
  public void setIndexMaxSize(int maxIndexSize) {
	  config.getIndex().setIndexMaxSize(maxIndexSize);
  }
  
  public int getArchiveThreads() {
	   return config.getArchiver().getArchiveThreads();
  }
  
  public void setArchiveThreads(int archiveThreads) {
	   config.getArchiver().setArchiveThreads(archiveThreads);
  }
  
  public List<String> getRoleValues() {
	  ArrayList<String> roleValues = new ArrayList<String>();
	  for (Roles.Role role : config.getRoles().getRoles()) {
		  if (Compare.equalsIgnoreCase(role.getName(),"system") ||
			  Compare.equalsIgnoreCase(role.getName(),"master"))
			  continue;
		  roleValues.add(role.getName());
	  }
	  return roleValues;
  }

	public List<String> getBindIPAddresses() {
		ArrayList<String> ipAddresses = new ArrayList<String>();
		ipAddresses.add("all");
		addIpAddresses(ipAddresses);
		return ipAddresses;
	  }
	
	
	protected void addIpAddresses(ArrayList<String> ipAddresses) {
		try {
			for (Enumeration e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();) {
				NetworkInterface iface = (NetworkInterface) e.nextElement();
				for (Enumeration a = iface.getInetAddresses(); a.hasMoreElements();) {
					InetAddress addr = (InetAddress) a.nextElement();
					ipAddresses.add(addr.getHostAddress());
				}
			}
		} catch (Exception e) {
			logger.error("failed to retrieve bind ip addresses:"+e.getMessage());
		}
	}
	public List<String> getBindIPAddressLabels() {
		ArrayList<String> ipAddresses = new ArrayList<String>();
		ipAddresses.add(getMessage("config.agent_all_interfaces"));
		addIpAddresses(ipAddresses);
		return ipAddresses;
	  }


 public boolean getDiskSpaceChecking() {
		  return config.getVolumes().getDiskSpaceChecking();
	  }
	  
	  public void setDiskSpaceChecking(boolean diskSpaceChecking) {
		  config.getVolumes().setDiskSpaceChecking(diskSpaceChecking);
	  }
	  
	  public int getMailboxMaxMessages() {
		  return config.getMailboxConnections().getMaxMessages();
	  }
	  
	  public void setMailboxMaxMessages(int maxMessages) {
		  this.config.getMailboxConnections().setMaxMessages(maxMessages);
	  }

	
      public List<LogFiles.LogFile> getLogFiles() { 
		  return config.getLogFiles().getLogFiles();
   	  }

	  public String getLog() {
		  return ConfigurationService.viewLog(logFile);
	  }
	  
	  public void setLogFile(String logFile) {
		  this.logFile = logFile;
	  }
	  
	  public String getLogFile() {
		  return this.logFile;
	  }
	  public String deleteLog(String logFile) {
		  ConfigurationService.deleteLog(logFile);
		  return "reload";
	  }
	
	  public void valueUnbound(HttpSessionBindingEvent event) {
		  logger.debug("configBean unbound");
		  config = null;
	  }
	  
	  public void valueBound(HttpSessionBindingEvent event) {
		  logger.debug("configBean bound");
		  config = new Config();	
	  }
	  
	  public int getMaxSearchResults() {
		  return config.getSearch().getMaxSearchResults();
	  }
	  
	  public void setMaxSearchResults(int maxSearchResults) {
		  config.getSearch().setMaxSearchResults(maxSearchResults);
	  }
	  
	  public List<String> getMaxSearchResultsList() {
		  ArrayList<String> list = new ArrayList<String>();
		  list.add("100");
		  list.add("500");
		  list.add("1000");
		  list.add("10000");
		  list.add("100000");
		  list.add("1000000");
		  list.add("5000000");
		  return list;
	  }
	  public List<String> getMaxSearchResultsLabels() {
		  ArrayList<String> list = new ArrayList<String>();
		  list.add("100");
		  list.add("500");
		  list.add("1000");
		  list.add("10,000");
		  list.add("100,000");
		  list.add("1,000,000");
		  list.add("5,000,000");
		  return list;
	  }
	  
	  
 public String getTempDir() { return config.getTempDir(); }
	    public void setTempDir(String tempDir) { config.setTempDir(tempDir); }
		  
	    
	    public boolean getProcessMalformedMessages() { return config.getArchiver().getProcessMalformedMessages(); }
	    public void setProcessMalformedMessages(boolean processMalformedMessages) {
	    	config.getArchiver().setProcessMalformedMessages(processMalformedMessages);
	    }
		 
}

