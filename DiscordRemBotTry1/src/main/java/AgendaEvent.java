public class AgendaEvent {
    // Klasse voor events in de agenda
        // Eigenschappen van een AgendaEvent
        private String title;
        private String date;
        private String time;

        // Constructor voor AgendaEvent
        public AgendaEvent(String title, String date, String time) {
            this.title = title;
            this.date = date;
            this.time = time;
        }



        // Getters voor de eigenschappen van AgendaEvent
        public String getTitle() {
            return title;
        }

        public String getDate() {
            return date;
        }

        public String getTime() {
            return time;
        }

        // Override van toString() om een AgendaEvent te kunnen weergeven als tekst
        @Override
        public String toString() {
            return title + " (" + date + " om " + time + ")";
        }

    }

