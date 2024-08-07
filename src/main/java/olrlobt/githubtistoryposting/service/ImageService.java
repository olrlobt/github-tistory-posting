package olrlobt.githubtistoryposting.service;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import olrlobt.githubtistoryposting.domain.BlogInfo;
import olrlobt.githubtistoryposting.domain.Posting;
import olrlobt.githubtistoryposting.domain.PostingBase;
import olrlobt.githubtistoryposting.utils.FontUtils;
import olrlobt.githubtistoryposting.utils.SvgUtils;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.svggen.SVGGraphics2D;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGDocument;

@Service
@Slf4j
public class ImageService {
    private final String TRUNCATE = "...";
    private final Color STROKE_COLOR = Color.decode("#d0d7de");
//    private final Color STROKE_COLOR = new Color(139, 139, 139, 34);

    public byte[] createSvgImageBox(Posting posting) throws IOException {
        SVGGraphics2D svgGenerator = SvgUtils.init();
        PostingBase postingBase = posting.getPostingBase();
        svgGenerator.setSVGCanvasSize(new java.awt.Dimension(postingBase.getBoxWidth(), postingBase.getBoxHeight()));
        drawBackground(svgGenerator, postingBase);
        drawThumbnail(posting, svgGenerator, posting.getPostingBase());
        drawText(posting, svgGenerator, posting.getPostingBase());
        drawFooter(posting, svgGenerator, postingBase);
        drawAuthor(posting, svgGenerator, postingBase);
        drawWatermark(posting, svgGenerator, postingBase);
        drawStroke(svgGenerator, postingBase);

        return SvgUtils.toByte(svgGenerator);
    }

    private void drawBackground(SVGGraphics2D svgGenerator, PostingBase postingBase) {
        svgGenerator.setPaint(Color.WHITE);

        RoundRectangle2D background = new RoundRectangle2D.Double(
                0, 0,
                postingBase.getBoxWidth(), postingBase.getBoxHeight(),
                postingBase.getBoxArcWidth(), postingBase.getBoxArcHeight()
        );
        svgGenerator.fill(background);
//        svgGenerator.fill(new Rectangle2D.Double(0, 0, postingType.getBoxWidth(), postingType.getBoxHeight()));
    }

    private void drawThumbnail(Posting posting, SVGGraphics2D svgGenerator, PostingBase postingBase) {
        String imageUrl = posting.getThumbnail();
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageUrl = BlogInfo.NOT_FOUND.getBlogThumb();
        }

        try {
            BufferedImage originalImage = ImageIO.read(new URL(imageUrl));
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            int targetWidth = postingBase.getImgWidth();
            int targetHeight = postingBase.getImgHeight();
            int targetX = postingBase.getImgX();
            int targetY = 0;
            double originalAspect = (double) originalWidth / originalHeight;
            double targetAspect = (double) targetWidth / targetHeight;

            int drawWidth, drawHeight;
            int xOffset = 0, yOffset = 0;

            if (originalAspect > targetAspect) { // 원본 이미지 width > height
                drawHeight = targetHeight;
                drawWidth = (int) (targetHeight * originalAspect);
                xOffset = (drawWidth - targetWidth) / 2;
            } else { // 원본 이미지 height > width
                drawWidth = targetWidth;
                drawHeight = (int) (targetWidth / originalAspect);
                yOffset = (drawHeight - targetHeight) / 2;
            }

            RoundRectangle2D roundedClip = new RoundRectangle2D.Double(
                    0, 0, postingBase.getBoxWidth(), postingBase.getBoxHeight(), postingBase.getBoxArcWidth(),
                    postingBase.getBoxArcHeight()
            );
            Rectangle2D rectClip = new Rectangle2D.Double(
                    targetX, targetY, targetWidth, targetHeight
            );

            Area combinedClip = new Area(roundedClip);
            combinedClip.intersect(new Area(rectClip));

            Shape originalClip = svgGenerator.getClip();
            svgGenerator.setClip(combinedClip);

            svgGenerator.drawImage(originalImage,
                    targetX - xOffset,
                    targetY - yOffset,
                    drawWidth,
                    drawHeight,
                    null);

            svgGenerator.setClip(originalClip);

        } catch (IOException e) {
            log.error("Failed to load image from URL: {}", imageUrl, e);
        }
    }

    private void drawText(Posting posting, SVGGraphics2D svgGenerator, PostingBase postingBase) {
        if (postingBase.getTitleY() == -1) {
            return;
        }

        svgGenerator.setPaint(Color.BLACK);
        if (postingBase.getTitleWeight() == 1) {
            svgGenerator.setFont(FontUtils.load_b(postingBase.getTitleSize()));
        } else {
            svgGenerator.setFont(FontUtils.load_m(postingBase.getTitleSize()));
        }

        int titleHeight = drawMultilineText(
                svgGenerator,
                posting.getTitle(),
                postingBase.getTextPadding(),
                postingBase.getTitleY(),
                postingBase.getTitleWidth() - postingBase.getTextPadding() * 2,
                postingBase.getTitleMaxLine(),
                svgGenerator.getFont()
        );

        if (postingBase.getContentMaxLine() != -1 && !posting.getContent().isEmpty()) {
            drawMultilineText(
                    svgGenerator,
                    posting.getContent(),
                    postingBase.getTextPadding(),
                    titleHeight,
                    postingBase.getTitleWidth() - postingBase.getTextPadding() * 2,
                    postingBase.getContentMaxLine(),
                    FontUtils.load_m(postingBase.getContentSize())
            );
        }
    }

    private static void drawFooter(Posting posting, SVGGraphics2D svgGenerator, PostingBase postingBase) {
        if (postingBase.getFooterType() == -1) {
            return;
        }

        svgGenerator.setFont(FontUtils.load_m());
        svgGenerator.setPaint(Color.GRAY);
        String footer = "";
        String publishedTime = posting.getPublishedTime();
        String url = posting.getUrl();
        int height = 0;
        if (postingBase.getFooterType() == 1) {
            footer = (publishedTime != null && !publishedTime.isEmpty()) ? publishedTime : url;
            height = (publishedTime != null && !publishedTime.isEmpty()) ? postingBase.getPublishedTimeY()
                    : postingBase.getUrlY();
        } else if (postingBase.getFooterType() == 2) {
            footer = (url != null && !url.isEmpty()) ? url : publishedTime;
            height = (url != null && !url.isEmpty()) ? postingBase.getUrlY()
                    : postingBase.getPublishedTimeY();
        } else if (postingBase.getFooterType() == 0) {
            footer = publishedTime;
            svgGenerator.drawString(footer, postingBase.getTextPadding(), postingBase.getPublishedTimeY());
            svgGenerator.setFont(FontUtils.load_b());
            footer = posting.getSiteName();
            height = postingBase.getUrlY();
        }
        svgGenerator.drawString(footer, postingBase.getTextPadding(), height);
    }

    private void drawAuthor(Posting posting, SVGGraphics2D svgGenerator, PostingBase postingBase) {
        if (postingBase.getBlogImageY() == -1) {
            return;
        }
        String imageUrl = posting.getBlogImage();

        if (imageUrl == null || imageUrl.isEmpty()) {
            imageUrl = BlogInfo.NOT_FOUND.getBlogThumb();
        }

        int width = 24;
        int height = 24;

        try {
            BufferedImage originalImage = ImageIO.read(new URL(imageUrl));
            BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2dMask = mask.createGraphics();
            g2dMask.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2dMask.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2dMask.fillOval(0, 0, width, height);
            g2dMask.dispose();

            BufferedImage circularImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = circularImage.createGraphics();

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.drawImage(originalImage, 0, 0, width, height, null);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_IN));
            g2d.drawImage(mask, 0, 0, null);
            g2d.dispose();

            svgGenerator.drawImage(circularImage, postingBase.getTextPadding(), postingBase.getBlogImageY(),
                    null);
        } catch (IOException e) {
            log.error("Failed to load image from URL: {}", imageUrl, e);
        }

        drawStroke(svgGenerator, 0, postingBase.getBoxWidth(),
                postingBase.getBlogImageY() - postingBase.getTextPadding() / 2,
                postingBase.getBlogImageY() - postingBase.getTextPadding() / 2 + 1);

        svgGenerator.setFont(FontUtils.load_m());
        String byText = "by ";
        svgGenerator.setPaint(Color.GRAY);
        svgGenerator.drawString(byText, postingBase.getTextPadding() + width * 3 / 2,
                postingBase.getBlogImageY() + height * 2 / 3);

        FontMetrics metrics = svgGenerator.getFontMetrics();
        int byTextWidth = metrics.stringWidth(byText);

        String authorText = posting.getAuthor();
        svgGenerator.setPaint(Color.BLACK);
        svgGenerator.drawString(authorText, postingBase.getTextPadding() + width * 3 / 2 + byTextWidth,
                postingBase.getBlogImageY() + height * 2 / 3);
    }

    private void drawWatermark(Posting posting, SVGGraphics2D svgGenerator, PostingBase postingBase) {
        if (posting.getWatermark() == null || postingBase.getWatermarkX() == -1
                || postingBase.getWatermarkY() == -1) {
            return;
        }

        Resource svgResource = posting.getWatermark().getResource();
        SVGDocument svgDocument = SvgUtils.loadSVGDocument(svgResource);

        if (svgDocument != null) {
            Element svgElement = svgDocument.getDocumentElement();
            changeSVGColor(svgElement, posting.getWatermark().getColor());

            UserAgentAdapter userAgent = new UserAgentAdapter();
            BridgeContext bridgeContext = new BridgeContext(userAgent);
            GraphicsNode svgGraphicsNode = new GVTBuilder().build(bridgeContext, svgDocument);

            AffineTransform scaleTransform = getScaleTransform(svgGraphicsNode,
                    posting.getPostingBase().getWatermarkWidth(),
                    posting.getPostingBase().getWatermarkHeight());

            AffineTransform transform = new AffineTransform();
            transform.translate(postingBase.getWatermarkX(), postingBase.getWatermarkY());
            transform.concatenate(scaleTransform);

            svgGraphicsNode.setTransform(transform);
            svgGraphicsNode.paint(svgGenerator);
        }
    }


    private void changeSVGColor(Element svgElement, String color) {
        // 모든 <circle> 요소의 색상 변경 (fill 속성)
        NodeList circles = svgElement.getElementsByTagName("circle");
        for (int i = 0; i < circles.getLength(); i++) {
            Element circle = (Element) circles.item(i);
            circle.setAttribute("fill", color);
        }

        // 추가로 다른 요소들도 색상 변경 적용 가능
        // 예: 모든 <path>, <rect> 등 요소의 색상 변경
        NodeList paths = svgElement.getElementsByTagName("path");
        for (int i = 0; i < paths.getLength(); i++) {
            Element path = (Element) paths.item(i);
            path.setAttribute("fill", color);
        }
    }

    private AffineTransform getScaleTransform(GraphicsNode svgGraphicsNode, double targetWidth, double targetHeight) {
        double originalWidth = svgGraphicsNode.getPrimitiveBounds().getWidth();
        double originalHeight = svgGraphicsNode.getPrimitiveBounds().getHeight();
        double scaleX = targetWidth / originalWidth;
        double scaleY = targetHeight / originalHeight;
        return AffineTransform.getScaleInstance(scaleX, scaleY);
    }

    private void drawStroke(SVGGraphics2D svgGenerator, PostingBase postingBase) {
        svgGenerator.setPaint(STROKE_COLOR);

        RoundRectangle2D stroke = new RoundRectangle2D.Double(
                0, 0,
                postingBase.getBoxWidth(), postingBase.getBoxHeight(),
                postingBase.getBoxArcWidth(), postingBase.getBoxArcHeight()
        );
        svgGenerator.draw(stroke);
    }

    private void drawStroke(SVGGraphics2D svgGenerator, int startX, int endX, int startY, int endY) {
        svgGenerator.setPaint(STROKE_COLOR);
        svgGenerator.draw(new Line2D.Double(startX, startY, endX, endY));
    }

    public int drawMultilineText(SVGGraphics2D svgGenerator, String text,
                                 int startX, int startY, int maxWidth, int maxLines, Font font) {
        FontMetrics metrics = svgGenerator.getFontMetrics(FontUtils.load_size(font.getSize()));
        int lineHeight = metrics.getHeight();
        svgGenerator.setFont(font);

        int linesCount = 0;
        StringBuilder currentLine = new StringBuilder();
        for (char ch : text.toCharArray()) {
            currentLine.append(ch);
            String lineText = currentLine.toString();
            double lineWidth = metrics.stringWidth(lineText) + startX;
            if (lineWidth > maxWidth || text.indexOf(ch) == text.length() - 1) {
                if (linesCount < maxLines - 1) {
                    svgGenerator.drawString(lineText, startX, startY + linesCount * lineHeight);
                    linesCount++;
                    currentLine = new StringBuilder();
                } else {
                    if (lineWidth > maxWidth) {
                        while (metrics.stringWidth(lineText + TRUNCATE) > maxWidth && lineText.length() > 0) {
                            lineText = lineText.substring(0, lineText.length() - 1);
                        }
                        lineText += TRUNCATE;
                    }
                    svgGenerator.drawString(lineText, startX, startY + linesCount * lineHeight);
                    linesCount++;
                    break;
                }
            }
        }

        if (currentLine.length() > 0 && linesCount < maxLines) {
            svgGenerator.drawString(currentLine.toString(), startX, startY + linesCount * lineHeight);
            linesCount++;
        }
        return startY + linesCount * lineHeight;
    }
}
