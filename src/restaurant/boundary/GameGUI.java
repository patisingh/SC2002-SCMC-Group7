package restaurant.boundary;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import restaurant.control.GameController;
import restaurant.entity.Customer;
import restaurant.entity.Money;
import restaurant.entity.Order;
import restaurant.entity.Restaurant;
import restaurant.entity.Table;

/**
 * Window version of the game, built with Swing.
 *
 * <p>The game controller runs on its own thread and calls these methods exactly as it
 * calls GameCLI. Display methods copy what they need from the model on the game thread,
 * then hand the drawing to the Swing thread. The ask methods show buttons and wait
 * until the manager clicks one (or presses its number key).
 */
public class GameGUI implements GameView {

    // Palette
    private static final Color BG = new Color(0xF4EFE7);
    private static final Color CARD = Color.WHITE;
    private static final Color INK = new Color(0x2B2622);
    private static final Color MUTED = new Color(0x8A8178);
    private static final Color LINE = new Color(0xE3DBCF);
    private static final Color ACCENT = new Color(0xC8552B);
    private static final Color ACCENT_DARK = new Color(0xA4421F);
    private static final Color GOOD = new Color(0x3E8E5E);
    private static final Color WARN = new Color(0xC98A1B);
    private static final Color BAD = new Color(0xC2413A);
    private static final Color INFO = new Color(0x3F6FB0);
    private static final Color HEADER = new Color(0x2B2622);

    private static final String FONT = Font.SANS_SERIF;

    private final JFrame frame = new JFrame("Restaurant Rush - Stage 3");
    private final JLabel turnLabel = label("Not started", 15, Font.BOLD, Color.WHITE);
    private final SlimBar turnBar = new SlimBar(ACCENT, new Color(0x4A423C));
    private final StatTile paidTile = new StatTile("PAID CUSTOMERS");
    private final StatTile revenueTile = new StatTile("REVENUE");
    private final StatTile avgTile = new StatTile("AVG SATISFACTION");
    private final StatTile leftTile = new StatTile("LEFT UNHAPPY");
    private final StatTile happyTile = new StatTile("HAPPY HOUR");
    private final RestaurantScene scene = new RestaurantScene();
    private final JTextPane log = new JTextPane();
    private final JLabel promptLabel = label(" ", 16, Font.BOLD, INK);
    private final JLabel promptHint = label(" ", 12, Font.PLAIN, MUTED);
    private final JPanel choicePanel = new JPanel();

    private final BlockingQueue<Integer> answers = new LinkedBlockingQueue<>();
    private final Map<Integer, JButton> shortcuts = new HashMap<>();
    private final List<JButton> activeButtons = new ArrayList<>();

    // Only touched by the game thread.
    private Restaurant current;
    private String lastStaff = "HOST";
    private Runnable restartAction;

    private GameGUI() {
        buildWindow();
    }

    /** Creates the window on the Swing thread and waits until it is ready. */
    public static GameGUI create() {
        GameGUI[] holder = new GameGUI[1];
        try {
            SwingUtilities.invokeAndWait(() -> holder[0] = new GameGUI());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while opening the window", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Could not open the window", e.getCause());
        }
        return holder[0];
    }

    /** What the "Play again" button runs once a game has ended. */
    public void setRestartAction(Runnable restartAction) {
        this.restartAction = restartAction;
    }

    public void show() {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    // ------------------------------------------------------------------ GameView

    @Override
    public void showWelcome(int maxTurns) {
        current = null;
        List<String> lines = DisplayText.welcomeLines(maxTurns);
        onSwing(() -> {
            clearLog();
            appendLog("RESTAURANT RUSH  -  Stage 3", Style.TITLE);
            for (String paragraph : joinWrappedLines(lines)) {
                appendLog(paragraph, Style.PLAIN);
            }
            turnLabel.setText("Turn 0 of " + maxTurns);
            turnBar.setFraction(0);
            scene.reset();
            renderEmptyStats();
            showButtons("Ready to open the restaurant?",
                    "Read the rules on the right, then start. Number keys also work for every choice.",
                    List.of(new Choice(1, "Start game", null, true, "start")));
        });
        waitForAnswer();
    }

    @Override
    public void showTurnHeader(int turn, int maxTurns) {
        onSwing(() -> {
            turnLabel.setText("Turn " + turn + " of " + maxTurns);
            turnBar.setFraction(turn / (double) maxTurns);
            scene.announce("Turn " + turn);
            appendLog(" ", Style.PLAIN);
            appendLog("TURN " + turn + " OF " + maxTurns, Style.HEADING);
        });
    }

    @Override
    public void showState(Restaurant restaurant) {
        current = restaurant;
        refreshBoard();
    }

    @Override
    public void showSection(String title) {
        refreshBoard();
        onSwing(() -> appendLog(title, Style.SECTION));
    }

    @Override
    public void showTurnSummary(Restaurant restaurant, int turn, int maxTurns) {
        current = restaurant;
        refreshBoard();
        String line = "Served " + restaurant.getServedCount()
                + " | Left " + restaurant.getDepartures()
                + " | Revenue " + Money.format(restaurant.getRevenueCents())
                + " | Avg " + DisplayText.formatAverage(restaurant.averageSatisfaction())
                + " | Turn " + turn + "/" + maxTurns;
        onSwing(() -> appendLog(line, Style.SUMMARY));
    }

    @Override
    public void showResult(Restaurant r, boolean victory, boolean paidMet, boolean revenueMet, boolean satisfactionMet) {
        current = r;
        refreshBoard();
        String[][] rows = {
            {"Paid customers >= " + GameController.TARGET_PAID, String.valueOf(r.paidCount()), met(paidMet)},
            {"Revenue >= " + Money.format(GameController.TARGET_REVENUE_CENTS),
                Money.format(r.getRevenueCents()), met(revenueMet)},
            {"Avg satisfaction >= " + (int) GameController.TARGET_AVG_SATISFACTION,
                DisplayText.formatAverage(r.averageSatisfaction()), met(satisfactionMet)},
        };
        int departures = r.getDepartures();
        onSwing(() -> {
            appendLog(" ", Style.PLAIN);
            appendLog(victory ? "*** VICTORY ***" : "*** DEFEAT ***", victory ? Style.WIN : Style.LOSE);
            for (String[] row : rows) {
                appendLog(row[0] + ":  " + row[1] + "  " + row[2], Style.PLAIN);
            }
            appendLog("Customers who left unhappy: " + departures, Style.PLAIN);
            showResultPanel(victory, rows, departures);
        });
    }

    @Override
    public void showMessage(String text) {
        refreshBoard();
        onSwing(() -> appendLog(text, Style.MESSAGE));
    }

    @Override
    public void showError(String text) {
        onSwing(() -> appendLog(text, Style.ERROR));
    }

    @Override
    public int askChoice(String title, List<String> options, String zeroLabel) {
        refreshBoard();
        List<Choice> choices = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            choices.add(Choice.fromLabel(i + 1, options.get(i), true));
        }
        choices.add(new Choice(0, zeroLabel, null, false, "choice-0"));
        int answer = ask(title, "Click a task, or press its number key.", choices);
        if (title.startsWith("HOST")) {
            lastStaff = "HOST";
        } else if (title.startsWith("WAITER")) {
            lastStaff = "WAITER";
        }
        String picked = answer == 0 ? zeroLabel : Choice.fromLabel(answer, options.get(answer - 1), true).main();
        onSwing(() -> appendLog(staffName(title) + ": " + picked, Style.CHOICE));
        return answer;
    }

    @Override
    public int askPick(String title, List<String> labels) {
        refreshBoard();
        List<Choice> choices = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            choices.add(new Choice(i + 1, labels.get(i), null, true, "choice-" + (i + 1)));
        }
        int answer = ask(title, "Pick one.", choices);
        onSwing(() -> appendLog("Picked " + labels.get(answer - 1), Style.CHOICE));
        return answer - 1;
    }

    // ------------------------------------------------------------------ asking

    private int ask(String title, String hint, List<Choice> choices) {
        answers.clear();
        waitForScene();
        onSwing(() -> showButtons(title, hint, choices));
        return waitForAnswer();
    }

    /** Lets the characters finish walking before the next question appears (at most a few seconds). */
    private void waitForScene() {
        try {
            SwingUtilities.invokeAndWait(() -> { });
            long end = System.currentTimeMillis() + 6000;
            while (scene.isBusy() && System.currentTimeMillis() < end) {
                Thread.sleep(20);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    private int waitForAnswer() {
        try {
            return answers.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Game stopped while waiting for a choice", e);
        }
    }

    private void answer(int number) {
        for (JButton b : activeButtons) {
            b.setEnabled(false);
        }
        shortcuts.clear();
        answers.offer(number);
    }

    // ------------------------------------------------------------------ snapshots

    private record StateSnap(int paid, int revenueCents, String avgText, double avg, boolean hasAvg,
                             int left, int happyRemaining, boolean happyActive, boolean happyAvailable,
                             RestaurantScene.Frame frame) {
    }

    /** Copies the model on the game thread, then redraws on the Swing thread. */
    private void refreshBoard() {
        if (current == null) {
            return;
        }
        StateSnap snap = snapshot(current, lastStaff);
        onSwing(() -> renderBoard(snap));
    }

    private static StateSnap snapshot(Restaurant r, String staffHint) {
        List<RestaurantScene.Guest> guests = new ArrayList<>();
        List<Customer> queue = r.getQueue();
        for (int i = 0; i < queue.size(); i++) {
            guests.add(guest(queue.get(i), i, -1));
        }
        List<Table> tables = r.getTables();
        for (int t = 0; t < tables.size(); t++) {
            Customer c = tables.get(t).getCustomer();
            if (c != null) {
                guests.add(guest(c, -1, t));
            }
        }
        var avg = r.averageSatisfaction();
        RestaurantScene.Frame frame = new RestaurantScene.Frame(guests, r.paidCount(), r.getRevenueCents(),
                r.getHappyHour().isActive(), staffHint);
        return new StateSnap(r.paidCount(), r.getRevenueCents(), DisplayText.formatAverage(avg),
                avg.orElse(0), avg.isPresent(), r.getDepartures(),
                r.getHappyHour().getRemainingTurns(), r.getHappyHour().isActive(),
                r.getHappyHour().canActivate(), frame);
    }

    private static RestaurantScene.Guest guest(Customer c, int queueIndex, int table) {
        Order o = c.getOrder();
        int done = 0;
        int total = 0;
        if (o != null) {
            String[] parts = o.getProgress().split("/");
            done = Integer.parseInt(parts[0]);
            total = Integer.parseInt(parts[1]);
        }
        return new RestaurantScene.Guest(c.getId(), c.getTypeName(), c.getSatisfaction(), queueIndex, table,
                c.getStatus(), o == null ? null : o.getItem().getName(), o != null && o.isReady(),
                done, total, o == null ? 0 : o.getBillCents());
    }

    // ------------------------------------------------------------------ building the window

    private void buildWindow() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1100, 720));
        frame.setSize(1280, 840);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.add(buildHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(14, 14));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));

        JPanel stats = new JPanel(new GridLayout(1, 0, 12, 0));
        stats.setOpaque(false);
        stats.add(paidTile);
        stats.add(revenueTile);
        stats.add(avgTile);
        stats.add(leftTile);
        stats.add(happyTile);

        JPanel board = new JPanel(new BorderLayout(14, 14));
        board.setOpaque(false);
        board.add(stats, BorderLayout.NORTH);
        JPanel sceneCard = new JPanel(new BorderLayout());
        sceneCard.setBackground(BG);
        sceneCard.setBorder(BorderFactory.createLineBorder(LINE));
        sceneCard.add(scene, BorderLayout.CENTER);
        board.add(sceneCard, BorderLayout.CENTER);

        body.add(board, BorderLayout.CENTER);
        body.add(buildLogCard(), BorderLayout.EAST);
        body.add(buildDecisionCard(), BorderLayout.SOUTH);
        root.add(body, BorderLayout.CENTER);

        frame.setContentPane(root);
        installNumberKeys(root);
        renderEmptyStats();
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(label("RESTAURANT RUSH", 22, Font.BOLD, Color.WHITE));
        titles.add(label("Stage 3  |  You are the manager: tell the Host, Waiter and Chef what to do",
                12, Font.PLAIN, new Color(0xC9BFB4)));
        header.add(titles, BorderLayout.WEST);

        JPanel turn = new JPanel();
        turn.setOpaque(false);
        turn.setLayout(new BoxLayout(turn, BoxLayout.Y_AXIS));
        turnLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        turnBar.setAlignmentX(Component.RIGHT_ALIGNMENT);
        turnBar.setPreferredSize(new Dimension(220, 8));
        turnBar.setMaximumSize(new Dimension(220, 8));
        turn.add(turnLabel);
        turn.add(Box.createVerticalStrut(8));
        turn.add(turnBar);
        header.add(turn, BorderLayout.EAST);
        return header;
    }

    private JComponent buildLogCard() {
        JPanel card = card(new BorderLayout(0, 8));
        card.add(label("WHAT'S HAPPENING", 12, Font.BOLD, MUTED), BorderLayout.NORTH);
        log.setEditable(false);
        log.setFocusable(false);
        log.setBackground(CARD);
        log.setFont(new Font(FONT, Font.PLAIN, 13));
        log.setName("log");
        JScrollPane scroll = new JScrollPane(log);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(380, 10));
        return card;
    }

    private JComponent buildDecisionCard() {
        JPanel card = card(new BorderLayout(0, 10));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        promptLabel.setName("prompt");
        text.add(promptLabel);
        text.add(Box.createVerticalStrut(2));
        text.add(promptHint);
        card.add(text, BorderLayout.NORTH);
        choicePanel.setOpaque(false);
        choicePanel.setName("choices");
        card.add(choicePanel, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(10, 150));
        return card;
    }

    private void installNumberKeys(JComponent root) {
        InputMap keys = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        for (int n = 0; n <= 9; n++) {
            int number = n;
            String key = "choice" + n;
            keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_0 + n, 0), key);
            keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0 + n, 0), key);
            root.getActionMap().put(key, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JButton b = shortcuts.get(number);
                    if (b != null && b.isEnabled()) {
                        b.doClick();
                    }
                }
            });
        }
    }

    // ------------------------------------------------------------------ rendering (Swing thread)

    private void renderEmptyStats() {
        paidTile.set("0", "target " + GameController.TARGET_PAID, false);
        revenueTile.set("$0.00", "target " + Money.format(GameController.TARGET_REVENUE_CENTS), false);
        avgTile.set("N/A", "target " + (int) GameController.TARGET_AVG_SATISFACTION, false);
        leftTile.set("0", "fewer is better", false);
        leftTile.setValueColor(INK);
        happyTile.set("Available", "once, for 2 turns", false);
        happyTile.setValueColor(INK);
    }

    private void renderBoard(StateSnap s) {
        paidTile.set(String.valueOf(s.paid()), "target " + GameController.TARGET_PAID,
                s.paid() >= GameController.TARGET_PAID);
        revenueTile.set(Money.format(s.revenueCents()), "target " + Money.format(GameController.TARGET_REVENUE_CENTS),
                s.revenueCents() >= GameController.TARGET_REVENUE_CENTS);
        avgTile.set(s.avgText(), "target " + (int) GameController.TARGET_AVG_SATISFACTION,
                s.hasAvg() && s.avg() >= GameController.TARGET_AVG_SATISFACTION);
        leftTile.set(String.valueOf(s.left()), "fewer is better", false);
        leftTile.setValueColor(s.left() > 0 ? BAD : INK);
        if (s.happyActive()) {
            happyTile.set("ACTIVE", s.happyRemaining() == 1 ? "last turn (this one)" : s.happyRemaining() + " turns left", false);
            happyTile.setValueColor(ACCENT);
        } else {
            happyTile.set(s.happyAvailable() ? "Available" : "Used", s.happyAvailable() ? "once, for 2 turns" : "used / expired", false);
            happyTile.setValueColor(s.happyAvailable() ? INK : MUTED);
        }

        scene.show(s.frame());
    }

    private void showButtons(String title, String hint, List<Choice> choices) {
        promptLabel.setText(title);
        promptHint.setText(hint);
        choicePanel.removeAll();
        activeButtons.clear();
        shortcuts.clear();
        int cols = Math.max(3, Math.min(4, choices.size()));
        choicePanel.setLayout(new GridLayout(0, cols, 10, 10));
        for (Choice c : choices) {
            ChoiceButton b = new ChoiceButton(c);
            b.addActionListener(e -> answer(c.number()));
            choicePanel.add(b);
            activeButtons.add(b);
            shortcuts.put(c.number(), b);
        }
        choicePanel.revalidate();
        choicePanel.repaint();
    }

    private void showResultPanel(boolean victory, String[][] rows, int departures) {
        promptLabel.setText(victory ? "VICTORY - every target met!" : "DEFEAT - not every target was met");
        promptLabel.setForeground(victory ? GOOD : BAD);
        promptLabel.setName("result-title");
        promptHint.setText("Customers who left unhappy: " + departures);
        choicePanel.removeAll();
        activeButtons.clear();
        shortcuts.clear();
        choicePanel.setLayout(new GridLayout(1, 0, 10, 0));
        for (String[] row : rows) {
            JPanel tile = new JPanel();
            tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
            tile.setBackground(new Color(0xFAF7F2));
            tile.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(LINE), BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            tile.add(left(label(row[0], 12, Font.PLAIN, MUTED)));
            JLabel value = label(row[1] + "   " + row[2], 18, Font.BOLD, "MET".equals(row[2]) ? GOOD : BAD);
            value.setName("result-row");
            tile.add(left(value));
            choicePanel.add(tile);
        }
        JPanel buttons = new JPanel(new GridLayout(2, 1, 0, 8));
        buttons.setOpaque(false);
        if (restartAction != null) {
            ChoiceButton again = new ChoiceButton(new Choice(1, "Play again", null, true, "again"));
            again.addActionListener(e -> {
                promptLabel.setForeground(INK);
                promptLabel.setName("prompt");
                again.setEnabled(false);
                restartAction.run();
            });
            buttons.add(again);
            shortcuts.put(1, again);
        }
        ChoiceButton quit = new ChoiceButton(new Choice(0, "Quit", null, false, "quit"));
        quit.addActionListener(e -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
        buttons.add(quit);
        shortcuts.put(0, quit);
        choicePanel.add(buttons);
        choicePanel.revalidate();
        choicePanel.repaint();
    }

    // ------------------------------------------------------------------ log

    private enum Style { TITLE, HEADING, SECTION, PLAIN, MESSAGE, ERROR, CHOICE, SUMMARY, WIN, LOSE }

    private void clearLog() {
        log.setText("");
    }

    private void appendLog(String text, Style style) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setFontFamily(a, FONT);
        StyleConstants.setFontSize(a, 13);
        StyleConstants.setForeground(a, INK);
        StyleConstants.setSpaceAbove(a, 1);
        String prefix = "";
        switch (style) {
            case TITLE -> { StyleConstants.setBold(a, true); StyleConstants.setFontSize(a, 15); }
            case HEADING -> { StyleConstants.setBold(a, true); StyleConstants.setForeground(a, ACCENT); }
            case SECTION -> { StyleConstants.setBold(a, true); StyleConstants.setForeground(a, MUTED); prefix = "-- "; }
            case MESSAGE -> prefix = "> ";
            case ERROR -> { StyleConstants.setForeground(a, BAD); prefix = "! "; }
            case CHOICE -> { StyleConstants.setForeground(a, INFO); StyleConstants.setItalic(a, true); prefix = "  you: "; }
            case SUMMARY -> { StyleConstants.setForeground(a, MUTED); StyleConstants.setFontSize(a, 12); }
            case WIN -> { StyleConstants.setBold(a, true); StyleConstants.setForeground(a, GOOD); StyleConstants.setFontSize(a, 16); }
            case LOSE -> { StyleConstants.setBold(a, true); StyleConstants.setForeground(a, BAD); StyleConstants.setFontSize(a, 16); }
            default -> { }
        }
        StyledDocument doc = log.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), prefix + text + "\n", a);
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
        log.setCaretPosition(doc.getLength());
    }

    // ------------------------------------------------------------------ small helpers

    private static void onSwing(Runnable r) {
        SwingUtilities.invokeLater(r);
    }

    /** The rules are line-broken for a 62-column console; rejoin sentences so the log can wrap them. */
    private static List<String> joinWrappedLines(List<String> lines) {
        List<String> out = new ArrayList<>();
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty()) {
                out.add(" ");
                continue;
            }
            int last = out.size() - 1;
            if (last >= 0 && !line.startsWith("-") && !out.get(last).isBlank()
                    && !out.get(last).endsWith(".") && !out.get(last).endsWith(":")) {
                out.set(last, out.get(last) + " " + line);
            } else {
                out.add(line);
            }
        }
        return out;
    }

    private static String met(boolean met) {
        return met ? "MET" : "MISSED";
    }

    private static String staffName(String title) {
        int dash = title.indexOf(" - ");
        String name = dash > 0 ? title.substring(0, dash) : title;
        return name.charAt(0) + name.substring(1).toLowerCase();
    }

    private static JLabel label(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT, style, size));
        l.setForeground(color);
        return l;
    }

    private static <T extends JComponent> T left(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private static JPanel card(java.awt.LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(CARD);
        Border line = BorderFactory.createLineBorder(LINE);
        p.setBorder(BorderFactory.createCompoundBorder(line, BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        return p;
    }

    /** One button's content: number, main text, optional detail in brackets. */
    private record Choice(int number, String main, String detail, boolean primary, String name) {
        static Choice fromLabel(int number, String label, boolean primary) {
            String text = label.trim();
            String detail = null;
            int open = text.lastIndexOf('[');
            if (open > 0 && text.endsWith("]")) {
                detail = text.substring(open + 1, text.length() - 1);
                text = text.substring(0, open).trim();
            }
            return new Choice(number, text.replaceAll("\\s+", " "), detail, primary, "choice-" + number);
        }
    }

    private static final class ChoiceButton extends JButton {
        private final boolean primary;

        ChoiceButton(Choice c) {
            primary = c.primary();
            setName(c.name());
            String key = "<span style='color:" + (primary ? "#F6D9CC" : "#8A8178") + "'>" + c.number() + "</span>&nbsp;&nbsp;";
            String detail = c.detail() == null ? ""
                    : "<br><span style='font-size:10px;color:" + (primary ? "#F6D9CC" : "#8A8178") + "'>"
                    + escape(c.detail()) + "</span>";
            setText("<html><b>" + key + escape(c.main()) + "</b>" + detail + "</html>");
            setFont(new Font(FONT, Font.PLAIN, 14));
            setForeground(primary ? Color.WHITE : INK);
            setHorizontalAlignment(SwingConstants.LEFT);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill;
            if (!isEnabled()) {
                fill = primary ? new Color(0xE2B7A5) : new Color(0xF1ECE4);
            } else if (getModel().isPressed() || getModel().isRollover()) {
                fill = primary ? ACCENT_DARK : new Color(0xF1ECE4);
            } else {
                fill = primary ? ACCENT : CARD;
            }
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            if (!primary) {
                g2.setColor(LINE.darker());
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            }
            g2.dispose();
            super.paintComponent(g);
        }

        private static String escape(String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }

    /** A thin rounded progress bar. */
    private static final class SlimBar extends JComponent {
        private final Color fill;
        private final Color track;
        private double fraction;

        SlimBar(Color fill, Color track) {
            this.fill = fill;
            this.track = track;
        }

        void setFraction(double fraction) {
            this.fraction = Math.max(0, Math.min(1, fraction));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = getHeight();
            g2.setColor(track);
            g2.fillRoundRect(0, 0, getWidth(), h, h, h);
            int w = (int) Math.round(getWidth() * fraction);
            if (w > 0) {
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, Math.max(w, h), h, h, h);
            }
            g2.dispose();
        }
    }

    /** One number in the stats row with its target underneath. */
    private static final class StatTile extends JPanel {
        private final JLabel value = label("-", 22, Font.BOLD, INK);
        private final JLabel target = label(" ", 11, Font.PLAIN, MUTED);

        StatTile(String title) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(CARD);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(LINE), BorderFactory.createEmptyBorder(10, 14, 10, 14)));
            setName("stat-" + title.toLowerCase().replace(' ', '-'));
            add(left(label(title, 11, Font.BOLD, MUTED)));
            add(Box.createVerticalStrut(4));
            add(left(value));
            add(Box.createVerticalStrut(2));
            add(left(target));
        }

        void set(String valueText, String targetText, boolean met) {
            value.setText(valueText);
            value.setForeground(met ? GOOD : INK);
            target.setText(met ? targetText + "  -  MET" : targetText);
            target.setForeground(met ? GOOD : MUTED);
        }

        void setValueColor(Color c) {
            value.setForeground(c);
        }
    }
}
