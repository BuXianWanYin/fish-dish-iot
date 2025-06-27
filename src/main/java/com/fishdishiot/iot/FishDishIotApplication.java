package com.fishdishiot.iot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FishDishIotApplication {
    public static void main(String[] args) {
        SpringApplication.run(FishDishIotApplication.class, args);
        System.out.println(
                "         _____     ___     _________                                               \n" +
                        "|_   _|  .'   `.  |  _   _  |                                              \n" +
                        "  | |   /  .-.  \\ |_/ | | \\_|  .--.  .---.  _ .--.  _   __  .---.  _ .--.  \n" +
                        "  | |   | |   | |     | |     ( (`\\]/ /__\\\\[ `/'`\\][ \\ [  ]/ /__\\\\[ `/'`\\] \n" +
                        " _| |_  \\  `-'  /    _| |_     `'.'.| \\__., | |     \\ \\/ / | \\__., | |     \n" +
                        "|_____|  `.___.'    |_____|   [\\__) )'.__.'[___]     \\__/   '.__.'[___]    \n"
        );
    }
}