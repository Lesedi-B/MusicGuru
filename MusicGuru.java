import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.*;

public class MusicGuru extends JFrame {

    private Clip clip;
    private FloatControl volumeControl;
    private javax.swing.Timer timer;

    private final java.util.List<File> songs = new ArrayList<>();
    private final java.util.List<File> filteredSongs = new ArrayList<>();
    private int index = 0;

    private JLabel songLabel, timeLabel;
    private JSlider progress, volume;
    private JList<String> playlist;
    private DefaultListModel<String> listModel;
    private JTextField searchField;
    private CircularWaveformPanel visualizer;

    private boolean shuffle = false;
    private boolean repeat = false;

    public MusicGuru() {
        setTitle("MusicGuru ✨");
        setSize(420, 760);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        applyDarkTheme();

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        add(root);

        songLabel = new JLabel("No song", SwingConstants.CENTER);
        songLabel.setFont(new Font("Arial", Font.BOLD, 16));
        songLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(songLabel);

        timeLabel = new JLabel("00:00 / 00:00", SwingConstants.CENTER);
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(timeLabel);

        progress = new JSlider(0, 100, 0);
        root.add(progress);

        visualizer = new CircularWaveformPanel();
        visualizer.setPreferredSize(new Dimension(320, 320));
        root.add(visualizer);

        JPanel controls = new JPanel();
        JButton prev = red("⏮");
        JButton play = red("▶");
        JButton pause = red("⏸");
        JButton stop = red("⏹");
        JButton next = red("⏭");
        JButton shuffleBtn = red("🔀");
        JButton repeatBtn = red("🔁");

        controls.add(prev);
        controls.add(play);
        controls.add(pause);
        controls.add(stop);
        controls.add(next);
        controls.add(shuffleBtn);
        controls.add(repeatBtn);
        root.add(controls);

        volume = new JSlider(0, 100, 80);
        root.add(volume);

        searchField = new JTextField();
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        searchField.setToolTipText("Search song...");
        root.add(searchField);

        listModel = new DefaultListModel<>();
        playlist = new JList<>(listModel);
        JScrollPane scroll = new JScrollPane(playlist);
        scroll.setPreferredSize(new Dimension(380, 140));
        root.add(scroll);

        loadStaticPlaylist();

        play.addActionListener(e -> play());
        pause.addActionListener(e -> pause());
        stop.addActionListener(e -> stop());
        next.addActionListener(e -> next());
        prev.addActionListener(e -> prev());
        shuffleBtn.addActionListener(e -> shuffle = !shuffle);
        repeatBtn.addActionListener(e -> repeat = !repeat);

        volume.addChangeListener(e -> setVolume(volume.getValue()));

        playlist.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                index = playlist.getSelectedIndex();
                if (index >= 0 && index < filteredSongs.size()) {
                    loadSong(filteredSongs.get(index));
                    play();
                }
            }
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });

        progress.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (clip != null) {
                    clip.setMicrosecondPosition(
                            clip.getMicrosecondLength() * progress.getValue() / 100);
                }
            }
        });

        timer = new javax.swing.Timer(40, e -> update());
        setVisible(true);
    }

    private JButton red(String t) {
        JButton b = new JButton(t);
        b.setBackground(Color.RED);
        b.setFocusPainted(false);
        return b;
    }

    // 🎵 STATIC PLAYLIST
    private void loadStaticPlaylist() {
        String[] names = {
            "Crackazat - Be Real.wav",
            "Julian Gomez - Love Song 28.wav",
            "BeatsbyHand - Gypsy Woman.wav",
            "Trick Me (Extended Mix).wav",
            "beatsbyhand - Say Yes ft. Rona Ray.wav",
            "Wapo Jije - House Headz.wav",
            "music2.wav",
            "Crackazat - Cant Blame A Soul.wav",
            "SGVO - Above Water.wav",
            "OddXperienc - Read My Lips.wav",
            "Youve Been Missing Me.wav",
            "Over You.wav"
        };

        songs.clear();
        for (String n : names) {
            File f = new File(n);
            if (f.exists()) songs.add(f);
        }

        filteredSongs.clear();
        filteredSongs.addAll(songs);
        refreshPlaylist();

        if (!filteredSongs.isEmpty()) loadSong(filteredSongs.get(0));
    }

    private void refreshPlaylist() {
        listModel.clear();
        for (File f : filteredSongs)
            listModel.addElement(f.getName());
    }

    private void filter() {
        String q = searchField.getText().toLowerCase();
        filteredSongs.clear();
        for (File f : songs)
            if (f.getName().toLowerCase().contains(q))
                filteredSongs.add(f);
        refreshPlaylist();
    }

    private void loadSong(File f) {
        try {
            if (clip != null) {
                clip.stop();
                clip.close();
            }

            AudioInputStream ais = AudioSystem.getAudioInputStream(f);
            clip = AudioSystem.getClip();
            clip.open(ais);

            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN))
                volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

            clip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP &&
                        clip.getMicrosecondPosition() >= clip.getMicrosecondLength()) {
                    if (repeat) play();
                    else next();
                }
            });

            songLabel.setText(f.getName());
            visualizer.loadAudio(f);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void play() {
        if (clip != null) {
            clip.start();
            timer.start();
        }
    }

    private void pause() {
        if (clip != null) {
            clip.stop();
            timer.stop();
        }
    }

    private void stop() {
        if (clip != null) {
            clip.stop();
            clip.setMicrosecondPosition(0);
            timer.stop();
            visualizer.reset();
        }
    }

    private void next() {
        if (filteredSongs.isEmpty()) return;
        index = shuffle
                ? new Random().nextInt(filteredSongs.size())
                : (index + 1) % filteredSongs.size();
        playlist.setSelectedIndex(index);
    }

    private void prev() {
        if (filteredSongs.isEmpty()) return;
        index = (index - 1 + filteredSongs.size()) % filteredSongs.size();
        playlist.setSelectedIndex(index);
    }

    private void update() {
        if (clip != null && clip.isRunning()) {
            long len = clip.getMicrosecondLength();
            long pos = clip.getMicrosecondPosition();
            progress.setValue((int) (100 * pos / len));
            timeLabel.setText(format(pos) + " / " + format(len));
            visualizer.advance();
        }
    }

    private String format(long micro) {
        long s = micro / 1_000_000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    private void setVolume(int v) {
        if (volumeControl != null) {
            volumeControl.setValue(
                    volumeControl.getMinimum() +
                            (volumeControl.getMaximum() - volumeControl.getMinimum()) * v / 100f);
        }
    }

    private void applyDarkTheme() {
        UIManager.put("Panel.background", new Color(12, 12, 12));
        UIManager.put("Label.foreground", Color.WHITE);
    }

    // 🌈 NEON GRADIENT VISUALIZER + ✨ BLOOM BLUR
    static class CircularWaveformPanel extends JPanel {

        private float[] samples;
        private int pos = 0;
        private float pulse = 0f;
        private float hue = 0f;

        public void loadAudio(File f) {
            try {
                AudioInputStream ais = AudioSystem.getAudioInputStream(f);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int r;
                while ((r = ais.read(buffer)) != -1)
                    out.write(buffer, 0, r);

                byte[] data = out.toByteArray();
                samples = new float[data.length / 2];
                for (int i = 0; i < samples.length; i++) {
                    int lo = data[i * 2] & 0xff;
                    int hi = data[i * 2 + 1];
                    samples[i] = (short) ((hi << 8) | lo) / 32768f;
                }
                pos = 0;
                pulse = 0f;
            } catch (Exception e) {
                samples = null;
            }
        }

        public void advance() {
            if (samples == null) return;

            float energy = 0f;
            for (int i = pos; i < pos + 512 && i < samples.length; i++)
                energy += samples[i] * samples[i];

            if (energy > 0.02f) pulse = 1.3f;
            pulse *= 0.9f;

            pos += 300;
            hue += 0.004f;
            repaint();
        }

        public void reset() {
            pos = 0;
            pulse = 0f;
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (samples == null) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            int baseR = Math.min(cx, cy) - 30;

            // ✨ BLOOM LAYERS (FAKE SHADER)
            for (int glow = 10; glow >= 1; glow--) {
                float alpha = glow / 25f;
                g2.setStroke(new BasicStroke(glow * 2f));
                g2.setColor(new Color(
                        Color.getHSBColor(hue, 1f, 1f).getRed(),
                        Color.getHSBColor(hue, 1f, 1f).getGreen(),
                        Color.getHSBColor(hue, 1f, 1f).getBlue(),
                        (int)(alpha * 255)));
                drawCircle(g2, cx, cy, baseR + (int)(pulse * 20));
            }
        }

        private void drawCircle(Graphics2D g2, int cx, int cy, int r) {
            for (int i = 0; i < 360; i++) {
                int idx = (pos + i) % samples.length;
                double a = Math.toRadians(i);
                float amp = samples[idx] * 80;
                int x1 = (int) (cx + Math.cos(a) * r);
                int y1 = (int) (cy + Math.sin(a) * r);
                int x2 = (int) (cx + Math.cos(a) * (r + amp));
                int y2 = (int) (cy + Math.sin(a) * (r + amp));
                g2.drawLine(x1, y1, x2, y2);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MusicGuru::new);
    }
}
