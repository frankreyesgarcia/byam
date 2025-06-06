package com.jcabi.ssh;

import com.jcabi.aspects.RetryOnFailure;
import com.jcabi.log.Logger;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.cactoos.io.TeeInput;
import org.cactoos.scalar.LengthOf;
import org.cactoos.scalar.Unchecked;
import org.cactoos.text.TextOf;
import org.cactoos.text.UncheckedText;

/**
 * Single SSH Channel.
 *
 * @since 1.0
 */
@ToString
@EqualsAndHashCode(of = "key", callSuper = true)
@SuppressWarnings("PMD.TooManyMethods")
public final class Ssh extends AbstractSshShell {

    /**
     * Default SSH port.
     */
    public static final int PORT = 22;

    /**
     * Private SSH key.
     */
    private final transient String key;

    /**
     * Private SSH key pass phrase.
     */
    private final transient String passphrase;

    /**
     * Constructor.
     * @param adr IP address
     * @param user Login
     * @param priv Private SSH key
     * @throws IOException If fails
     * @since 1.4
     */
    public Ssh(final String adr, final String user, final URL priv)
        throws IOException {
        this(adr, Ssh.PORT, user, priv);
    }

    /**
     * Constructor.
     * @param adr IP address
     * @param user Login
     * @param priv Private SSH key
     * @throws IOException If fails
     * @since 1.4
     */
    public Ssh(final InetAddress adr, final String user, final URL priv)
        throws IOException {
        this(adr, Ssh.PORT, user, priv);
    }

    /**
     * Constructor.
     * @param adr IP address
     * @param user Login
     * @param priv Private SSH key
     * @throws UnknownHostException If fails
     * @since 1.4
     */
    public Ssh(final String adr, final String user, final String priv)
        throws UnknownHostException {
        this(adr, Ssh.PORT, user, priv);
    }

    /**
     * Constructor.
     * @param adr IP address
     * @param user Login
     * @param priv Private SSH key
     * @throws UnknownHostException If fails
     * @since 1.4
     */
    public Ssh(final InetAddress adr, final String user, final String priv)
        throws UnknownHostException {
        this(adr.getCanonicalHostName(), Ssh.PORT, user, priv);
    }

    /**
     * Constructor.
     * @param adr IP address
     * @param prt Port of server
     * @param user Login
     * @param priv Private SSH key
     * @throws IOException If fails
     * @since 1.4
     */
    public Ssh(final String adr, final int prt,
        final String user, final URL priv) throws IOException {
        this(adr, prt, user, new UncheckedText(new TextOf(priv)).asString());
    }

    /**
     * Constructor.
     * @param adr IP address
     * @param prt Port of server
     * @param user Login
     * @param priv Private SSH key
     * @throws IOException If fails
     * @since 1.4
     */
    public Ssh(final InetAddress adr, final int prt,
        final String user, final URL priv) throws IOException {
        this(
            adr.getCanonicalHostName(), prt, user,
            new UncheckedText(new TextOf(priv)).asString()
        );
    }

    /**
     * Constructor.
     * @param adr IP address
     * @param prt Port of server
     * @param user Login
     * @param priv Private SSH key
     * @throws UnknownHostException If fails
     */
    public Ssh(final String adr, final int prt,
        final String user, final String priv) throws UnknownHostException {
        this(adr, prt, user, priv, null);
    }

    /**
     * Constructor.
     * @param adr IP address
     * @param prt Port of server
     * @param user Login
     * @param priv Private SSH key
     * @param passphrs Pass phrase for encrypted priv. key
     * @throws UnknownHostException when host is unknown.
     */
    public Ssh(final String adr, final int prt,
        final String user, final String priv,
        final String passphrs
    ) throws UnknownHostException {
        super(adr, prt, user);
        this.key = priv;
        this.passphrase = passphrs;
    }

    /**
     * Escape SSH argument.
     * @param arg Argument to escape
     * @return Escaped
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static String escape(final String arg) {
        return String.format("'%s'", arg.replace("'", "'\\''"));
    }

    @Override
    @RetryOnFailure(
        attempts = 7,
        delay = 1,
        unit = TimeUnit.MINUTES,
        verbose = false,
        types = IOException.class
    )
    protected Session session() throws IOException {
        final File file = File.createTempFile("jcabi-ssh", ".key");
        try {
            JSch.setLogger(new JschLogger());
            final JSch jsch = new JSch();
            new Unchecked<>(
                new LengthOf(
                    new TeeInput(
                        this.key.replaceAll("\r", "")
                            .replaceAll("\n\\s+|\n{2,}", "\n")
                            .trim(),
                        file
                    )
                )
            ).value();
            jsch.setHostKeyRepository(new EasyRepo());
            if (this.passphrase == null) {
                jsch.addIdentity(file.getAbsolutePath());
            } else {
                jsch.addIdentity(
                    this.getLogin(),
                    this.key.getBytes(StandardCharsets.UTF_8),
                    null,
                    this.passphrase.getBytes(StandardCharsets.UTF_8)
                );
            }
            Logger.debug(
                this,
                "Opening SSH session to %s@%s:%s (%d bytes in RSA key)...",
                this.getLogin(), this.getAddr(), this.getPort(),
                file.length()
            );
            return this.session(jsch);
        } catch (final JSchException ex) {
            throw new IOException(ex);
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    /**
     * Make session.
     * @param sch The JSch
     * @return The session
     * @throws JSchException If fails
     */
    private Session session(final JSch sch) throws JSchException {
        final Session session = sch.getSession(
            this.getLogin(), this.getAddr(), this.getPort()
        );
        session.setConfig("StrictHostKeyChecking", "no");
        session.setTimeout((int) TimeUnit.MINUTES.toMillis(1L));
        session.setServerAliveInterval((int) TimeUnit.SECONDS.toMillis(1L));
        session.setServerAliveCountMax(1000000);
        session.connect((int) TimeUnit.SECONDS.toMillis(10L));
        Logger.debug(
            this,
            "SSH session opened to %s@%s:%s",
            this.getLogin(), this.getAddr(), this.getPort()
        );
        return session;
    }
}