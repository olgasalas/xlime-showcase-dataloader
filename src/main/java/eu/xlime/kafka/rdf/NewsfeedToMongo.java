package eu.xlime.kafka.rdf;

import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kafka.message.MessageAndMetadata;

import com.google.common.collect.ImmutableList;
import com.hp.hpl.jena.query.Dataset;

import eu.xlime.bean.EntityAnnotation;
import eu.xlime.bean.MediaItem;
import eu.xlime.bean.NewsArticleBean;
import eu.xlime.bean.XLiMeResource;
import eu.xlime.dao.annotation.MediaItemAnnotationDaoFromDataset;
import eu.xlime.kafka.ConfigOptions;
import eu.xlime.kafka.RunExtractor;
import eu.xlime.kafka.msgproc.DatasetProcessor;

/**
 * {@link DatasetProcessor} meant to be used in an xLiMe Kafka {@link RunExtractor} execution.
 *  
 * It expects a {@link Dataset} from an xLiMe (news article) stream and it will extract {@link NewsArticleBean}s;
 * then it will push those beans to a MongoDB. See {@link ConfigOptions} for configuration keys.
 *  
 * @author rdenaux
 *
 */
public class NewsfeedToMongo extends BaseXLiMeResourceToMongo {

	private static final Logger log = LoggerFactory.getLogger(NewsfeedToMongo.class);
	
	public NewsfeedToMongo(Properties props) {
		super(props);
	}

	@Override
	protected List<? extends XLiMeResource> extractXLiMeResourceBeans(
			MessageAndMetadata<byte[], byte[]> mm, Dataset dataset) {
		ImmutableList.Builder<XLiMeResource> builder = ImmutableList.builder();
		try {
			List<MediaItem> newsArts = extractXLiMeMediaItems(mm, dataset, NewsArticleBean.class); 
			builder.addAll(newsArts);
			for (MediaItem mit: newsArts) {
				builder.addAll(extractNewsArticleEntityAnnotations(dataset, mit.getUrl()));
			}
			//TODO: add activities
			//TODO: add topic annotations?
		} catch (Exception e) {
			log.error("Error processing message " + mm.offset(), e);
		}
		return builder.build(); 
	}

	private List<EntityAnnotation> extractNewsArticleEntityAnnotations(Dataset dataset, String newsArticleUrl) {
		try {
			MediaItemAnnotationDaoFromDataset testObj = new MediaItemAnnotationDaoFromDataset(dataset, kbEntityMapper);
			return testObj.findNewsArticleEntityAnnotations(newsArticleUrl);
		} catch (Exception e) {
			log.error("Failed to extract EntityAnnotations for " + newsArticleUrl, e);
			return ImmutableList.of();
		}
	}

}
