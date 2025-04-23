package com.example.marksman.service;

import com.example.marksman.model.Winner;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

public class WinnerService {
    private static final Logger LOGGER = Logger.getLogger("WinnerService");
    private static final SessionFactory sessionFactory;

    static {
        try {
            LOGGER.info("Инициализация SessionFactory...");
            sessionFactory = new Configuration()
                    .configure("hibernate.cfg.xml")
                    .buildSessionFactory();
            LOGGER.info("SessionFactory успешно инициализирован");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Ошибка инициализации SessionFactory", e);
            throw new ExceptionInInitializerError(e);
        }
    }

    public void saveWinner(Winner winner) {
        LOGGER.info("Попытка сохранения победителя: " + winner.getUsername());
        try (Session session = sessionFactory.openSession()) {
            LOGGER.info("Открытие сессии для сохранения победителя");
            session.beginTransaction();
            LOGGER.info("Начало транзакции");
            
            session.persist(winner);
            LOGGER.info("Победитель успешно сохранен в сессии");
            
            session.getTransaction().commit();
            LOGGER.info("Транзакция успешно завершена");
            
            LOGGER.info("Сохранен победитель: " + winner.getUsername() + 
                      " (ID: " + winner.getId() + 
                      ", Очки: " + winner.getScore() + 
                      ", Игроков: " + winner.getPlayersCount() + ")");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Ошибка при сохранении победителя", e);
            throw new RuntimeException("Не удалось сохранить победителя", e);
        }
    }

    public List<Object[]> getLeaderboard() {
        LOGGER.info("Получение таблицы лидеров");
        try (Session session = sessionFactory.openSession()) {
            LOGGER.info("Открытие сессии для получения таблицы лидеров");
            
            String hql = "SELECT w.username, COUNT(w.id) as wins " +
                        "FROM Winner w " +
                        "GROUP BY w.username " +
                        "ORDER BY wins DESC";
            
            List<Object[]> leaderboard = session.createQuery(hql, Object[].class)
                    .getResultList();
            
            LOGGER.info("Успешно получено " + leaderboard.size() + " записей в таблице лидеров");
            return leaderboard;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Ошибка при получении таблицы лидеров", e);
            throw new RuntimeException("Не удалось получить таблицу лидеров", e);
        }
    }

    public static void shutdown() {
        if (sessionFactory != null) {
            try {
                LOGGER.info("Закрытие SessionFactory...");
                sessionFactory.close();
                LOGGER.info("SessionFactory успешно закрыт");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Ошибка при закрытии SessionFactory", e);
            }
        }
    }
} 