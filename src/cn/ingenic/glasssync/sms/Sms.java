package cn.ingenic.glasssync.sms;

import android.net.Uri;

public class Sms {
	
	private static final Uri MMSSMS_CONTENT_URI = Uri
			.parse("content://mms-sms/");
	private static final Uri THREAD_CONTENT_URI = Uri.withAppendedPath(
			MMSSMS_CONTENT_URI, "conversations");
	
	
	public interface Thread{
		public final Uri sAllThreadsUri =
				THREAD_CONTENT_URI.buildUpon().appendQueryParameter("simple", "true").build();
		public static String ID="_id";
		public static String DATE="date";
		public static String MESSAGE_COUNT="message_count";
		public static String SNIPPET="snippet";
		public static String RECIPIENT_IDS="recipient_ids";	
		public static String READ="read";
		public static String TYPE="type";
		public static String ERROR="error";
		public static String RECIPIENT_ADS_KEY="recipient_ads";
	}
	
	public interface MidColumns{
		public static String READ="read";
		public static String TYPE="type";
		public static String BODY="body";
		public static String ERROR_CODE="error_code";
	}
	
	public interface SendColumns{
		
		public static String ADDRESS="address";
		public static String DATE="date";
		public static String READ="read";
		public static String TYPE="type";
		public static String BODY="body";
		public static String ERROR_CODE="error_code";
		public static String SEEN="seen";
		public static String THREAD_ID="thread_id";
	}
	public static String ID="_id";
	
	public static String THREAD_KEY="phone_thread_id";
//	
//	public interface SmsKey{
//		public static String ID_KEY="s_id";
//		public static String ADDRESS_KEY="s_address";
//		public static String BODY_KEY="s_body";
//		public static String DATE_KEY="s_data";
//		public static String READ_KEY="s_read_key";
//		public static String TYPE_KEY="s_type_key";
//		public static String ERROR_CODE_KEY="s_error_code";
//		public static String SEEN_KEY="s_seen";
//		public static String PHONE_THREAD_ID_KEY="phone_thread_id_key";
//	}
//	
//	public interface ThreadKey{
//		public static String ID_KEY="t_id";
//		public static String DATE_KEY="t_date_key";
//		public static String MESSAGE_COUNT_KEY="t_message_count_key";
//		public static String SNIPPET_KEY="t_snippet";
//		public static String RECIPIENT_IDS_KEY="t_recipient_ids";
//		public static String READ_KEY="t_read";
//		public static String TYPE_KEY="t_type";
//		public static String ERROR_KEY="t_error_key";
//		public static String SMS_ID="t_sms_id";
//		public static String PHONE_THREAD_ID_KEY="phone_thread_id_key";
//	}
	
	
	public interface Canonical{
		public static  Uri sAllCanonical =
	            Uri.parse("content://mms-sms/canonical-addresses");
		public static String ID="_id";
		public static String ADDRESS="address";
	}
	

}
