import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.MessageChannel;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ExampleBot {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static void main(final String[] args) {

        final String token = args[0];
        final DiscordClient client = DiscordClient.create(token);
        final GatewayDiscordClient gateway = client.login().block();
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        gateway.on(MessageCreateEvent.class).subscribe(test -> {
            final Message message = test.getMessage();

            if ("!ping".equals(message.getContent())) {
                final MessageChannel channel = message.getChannel().block();
                channel.createMessage("Pong!").block();

            }
            if ("/agenda".equals(message.getContent())) {

                // Lees het CSV-bestand in en sla de gegevens op in een lijst van events
                List<AgendaEvent> events = readCSVFile("C:\\Users\\Noah\\IdeaProjects\\DiscordRemBotTry1\\AgendaData.csv");

                // Make a overview of events
                StringBuilder messageBuilder = new StringBuilder();
                messageBuilder.append("Overzicht van events:\n");
                for (AgendaEvent event : events) {
                    messageBuilder.append(event.toString() + "\n");
                }
                String agendaMessage = messageBuilder.toString();

                // send message to user
                final MessageChannel channel = message.getChannel().block();
                channel.createMessage(agendaMessage).block();

            }
            if (message.getContent().startsWith("/addevent")) {
                // Split the message into separate parts
                String[] parts = message.getContent().split(" ");

                // Check if the message has the correct number of parts (4)
                if (parts.length != 4) {
                    final MessageChannel channel = message.getChannel().block();
                    channel.createMessage("Invalid event format. Please use the following format: '/addevent title date time'").block();
                    return;
                }

                // Create a new AgendaEvent object with the given title, date, and time
                AgendaEvent event = new AgendaEvent(parts[1], parts[2], parts[3]);

                // Add the event to the CSV file
                try (BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\Users\\Noah\\IdeaProjects\\DiscordRemBotTry1\\AgendaData.csv", true))) {
                    bw.write(event.getTitle() + "," + event.getDate() + "," + event.getTime() + "\n");
                    bw.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Get the current date and time
                LocalDateTime now = LocalDateTime.now();

                // Calculate the number of days until the event
                LocalDate eventDate = LocalDate.parse(event.getDate(), formatter);
                long daysUntilEvent = ChronoUnit.DAYS.between(now.toLocalDate(), eventDate);

                // Get the role object for the RemBotUser role
                Snowflake roleId = Snowflake.of("1054779034841665677");
                Role role = message.getGuild().block().getRoleById(roleId).block();

                // Create a mention for the role
                String mention = role.getMention();

                // Schedule a task to send the reminder message at the appropriate time
                if (daysUntilEvent == 2) {
                    // Schedule the task to run in two days
                    executor.schedule(() -> {
                        final MessageChannel channel = message.getChannel().block();
                        channel.createMessage(mention + " " + event.getTitle() + " is due in 2 days").block();
                    }, 2, TimeUnit.DAYS);
                } else if (daysUntilEvent < 2) {
                    // The event is in less than two days, so send the message immediately
                    final MessageChannel channel = message.getChannel().block();
                    channel.createMessage(mention + " " + event.getTitle() + " is due in less than 2 days").block();
                }

                // Confirm to the user that the event was added
                final MessageChannel channel = message.getChannel().block();
                channel.createMessage("Event added to the agenda: " + event).block();
            }

            if (message.getContent().startsWith("/agendaremover")) {
// Extract the date argument from the message content
                String[] parts = message.getContent().split(" ");
                String date = parts[1];

                // Read the CSV file and create a list of AgendaEvent objects
                List<AgendaEvent> events = readCSVFile("C:\\Users\\Noah\\IdeaProjects\\DiscordRemBotTry1\\AgendaData.csv");

                // Remove all events with a date that is earlier than or equal to the specified date
                events.removeIf(event -> event.getDate().compareTo(date) <= 0);

                // Write the updated list of events back to the CSV file
                try (BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\Users\\Noah\\IdeaProjects\\DiscordRemBotTry1\\AgendaData.csv"))) {
                    for (AgendaEvent event : events) {
                        bw.write(event.getTitle() + "," + event.getDate() + "," + event.getTime() + "\n");
                    }
                    bw.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                final MessageChannel channel = message.getChannel().block();
                channel.createMessage("all events for " + date + " and before were deleted").block();
            }

            if (message.getContent().startsWith("/agendasingleremover")) {
                //extract argument to remove one event
                String[] parts = message.getContent().split(" ");
                String date = parts[2];
                String naam = parts[1];

                //go in the csv file
                List<AgendaEvent> events = readCSVFile("C:\\Users\\Noah\\IdeaProjects\\DiscordRemBotTry1\\AgendaData.csv");

                //removes specific event looking at name and date
                events.removeIf(event -> event.getDate().compareTo(date) == 0 && event.getTitle().compareTo(naam) == 0);

                // Write the updated list of events back to the CSV file
                try (BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\Users\\Noah\\IdeaProjects\\DiscordRemBotTry1\\AgendaData.csv"))) {
                    for (AgendaEvent event : events) {
                        bw.write(event.getTitle() + "," + event.getDate() + "," + event.getTime() + "\n");
                    }
                    bw.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Confirm to the user that the event was added
                final MessageChannel channel = message.getChannel().block();
                channel.createMessage(naam + " was correctly deleted.").block();
            }

            if ("/cleartxt".equals(message.getContent())) {
                final MessageChannel channel = message.getChannel().block();
                // Get the 50 most recent messages in the channel
                List<Message> messages = channel.getMessagesBefore(message.getId()).take(50).collectList().block();
                // Delete the messages one at a time
                for (Message m : messages) {
                    m.delete().block();
                }
            }

            if ("/help RemBot".equals(message.getContent())) {
                String s = "List Rembot\n";
                s += "```\n";
                s += "             Command | uitleg\n";
                s += "---------------------|--------------------------------------\n";
                s += "/agenda              | Toont alle events in agenda\n";
                s += "/addevent            | Kan je een event toevoegen in java\n";
                s += "/agendaremover       | Kan je events verwijderen op datum\n";
                s += "/agendasingleremover | kan je een specifiek event verwijderen\n";
                s += "/cleartxt            | kan je 50 messages clearen automatisch\n";
                s += "```";

                final MessageChannel channel = message.getChannel().block();
                channel.createMessage(s).block();
            }
        });

        gateway.onDisconnect().block();
    }

    public static List<AgendaEvent> readCSVFile(String fileName) {
        // Maak een lijst om de events op te slaan
        List<AgendaEvent> events = new ArrayList<>();

        // Lees het CSV-bestand in en sla de gegevens op in de lijst van events
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("Reading line: " + line);
                // Splits de regel in kolommen
                String[] columns = line.split(",");

                // Maak een nieuw Event-object en voeg het toe aan de lijst
                AgendaEvent event = new AgendaEvent(columns[0], columns[1], columns[2]);
                events.add(event);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Retourneer de lijst van events
        return events;
    }
}
