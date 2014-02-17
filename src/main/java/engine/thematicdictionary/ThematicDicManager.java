package engine.thematicdictionary;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.AbstractEntityPersister;

import util.HibernateUtil;
import engine.foundedwords.WordInfo;
import entities.Rubric;
/**
 * 
 */
public final class ThematicDicManager {
	
	private static ThematicDicManager instance;
	
	private List<Rubric> thematicDicts = new ArrayList<Rubric>();
	private Session session;
	private SessionFactory sessions = HibernateUtil.getSessionFactory();
	private Criteria crit;
	private Transaction tx;

	public static ThematicDicManager getInstance(){
		if( instance==null ){
		      instance = new ThematicDicManager();
		}
		return instance;
	}
	
	
	/**
	 * Конструктор
	 */
	private ThematicDicManager(){
		session = sessions.openSession();
		begin();
    	end();
	}
	
	public List<Rubric> getAllDicts() {
		return thematicDicts;
	}	
	
	public void addDic(Rubric thematicDic) {
		begin();
		session.save(thematicDic);
		end();
    }

	public Rubric getDic(int i) {
		return thematicDicts.get(i);
	}

	public void deleteDic(int dicIndex) {
		// Каскадные удаления для -- применять(дописать в класс Rubric), когда в Рубриках появятся Вероятности
		// http://docs.jboss.org/hibernate/orm/4.3/manual/en-US/html_single/#objectstate-transitive
		begin();
		session.delete(thematicDicts.get(dicIndex));
		end();
	}
	
	
	/**
	 * Стартует сессию
	 */
	private void begin(){
		tx = session.beginTransaction();
	}
	
	/**
	 * Обновляет список рубрик и закрывает сессию.
	 */
	private void end() {
		thematicDicts.clear();
		crit = session.createCriteria(Rubric.class); // создаем критерий //
														// запроса
		thematicDicts.addAll(crit.list());
		tx.commit();

		session.flush();
	}	

	
	
	
	
	
	
	
	
	
	

	public void addDic(String dicname, boolean isEnabled){
		Rubric r = new Rubric(dicname, isEnabled);
		addDic(r);
	}
		
	public void turn(boolean b, int index) {
		this.getDic(index).setDicEnabled(b);
	}

	public void addWord(int dicIndex, String word, double probability){
		//this.getDic(dicIndex).addWord(word, probability);
	}
	
	public void deleteWord(String word, int dicIndex) {
		//this.getDic(dicIndex).deleteWord(word);
	}
	
	/**
	 * Удаление файла БД
	 * @throws Exception 
	 */
	public void remove() throws Exception{
		String home = "";
		String path = HibernateUtil.dbFileName;
		final String ext = ".h2.db";
		if(HibernateUtil.dbFileName.startsWith("~")){
			home = System.getProperty("user.home");
			System.out.println("user.home: "+ home);
			path = path.substring(1);
		}
		File file = new File(home+path+ext);
		boolean isException = false;
		if(file.exists()){
			System.out.println("сейчас будет удалён файл базы данных: "+ file.getAbsolutePath());
			boolean success = file.delete();
			System.out.println("Файл удалён: " + success);
			isException = !success;
		}else{
			System.out.println("не удалось найти файл базы данных: "+ file.getAbsolutePath());
			isException = true;
		}
		if(isException){
			throw new Exception("Проблемы при удалении файла БД.");
		}
	}
	
	/**
	 * Очистка БД -- Удаляет содержимое всех таблиц
	 */
	public void clearDb() {
		System.out.println("Очистка БД...");

		Map<String, ClassMetadata> m = HibernateUtil.getSessionFactory().getAllClassMetadata();

		// iterate map :
		// http://stackoverflow.com/questions/46898/how-do-i-iterate-over-each-entry-in-a-map/46905#46905
		Iterator entries = m.entrySet().iterator();
		while (entries.hasNext()) {
			Entry thisEntry = (Entry) entries.next();
			Object key = thisEntry.getKey();
			Object value = thisEntry.getValue();
			System.out.println("\tСохранённый класс " + key + "; " + value);

			// Get all table names set up in SessionFactory
			// http://stackoverflow.com/questions/4813122/get-all-table-names-set-up-in-sessionfactory
			AbstractEntityPersister aep = (AbstractEntityPersister) m.get(key);
			String tableName = aep.getTableName();
			System.out.println("\tДля класса: " + key + " Имя таблицы: "
					+ tableName);
			System.out.println("\tУдаляю содержимое таблицы " + tableName);
			SQLQuery q = session.createSQLQuery("DELETE " + tableName + ";");
			q.executeUpdate();
		}

		begin();
		end();
	}
	
	
	/**
	 * Вычисляет вероятность того, что данный текст относится к рубрике, представленной словарём.
	 * @param dic - Тематический словарь, представляющий данную рубрику
	 * @param stems - стемы(начальные формы)
	 * @return вероятность
	 */
	public void calcProbabilityforDic(Rubric dic, Collection<WordInfo> stems) {
		double p = 0;
		int size = 0;
		
		for(WordInfo word : stems){
			String s = word.getString();
			int count = word.getCount();
			p += (getProbability4Word(dic, s) * count);
			
			size+=count;
		}
		p /= size;
		
		dic.setCalculatedProbability(p);
	}
	
	private double getProbability4Word(Rubric r, String s){
		return 0.0;
	}
	
	public static void checkProbabilityBounds(double probability){
		if (probability>1.0 || probability<0.0) throw new IllegalArgumentException("Вероятность может быть только 0.0...1.0");
	}

	/**
	 * Закрытие фабрики сессий, влекущее за собой закрытие соединения с БД.
	 */
	public void terminate() {
		session.close();
		sessions.close();
	}

}